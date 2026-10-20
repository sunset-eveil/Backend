# CLAUDE.md

> Claude Code용 컨텍스트 문서. 아래 내용은 팀에서 **확정된 결정**만 담고 있다.

> 여기에 없는 사항은 임의로 정하지 말고 "미결정 사항" 섹션을 보거나 질문할 것.

---

## 1. 서비스 요약

사용자가 AI(ChatGPT 등)에 보낼 텍스트에서 개인정보를 탐지해 `NAME1`, `PHONE1` 같은 **마스킹 코드**로 치환하고, 그 매핑(`NAME1 = 홍길동`)을 **서버가 암호화해 보관**하는 크롬 확장 프로그램의 백엔드다.

- 비회원 서비스. 로그인 없이 `clientId` + `authKey`로 클라이언트를 구분·인증한다.
- 핵심 보안 원칙: **서버 단독으로는 저장된 매핑을 절대 복호화할 수 없다.** 복호화에 필요한 KEK는 클라이언트가 요청 시에만 보내고, 서버는 처리 후 즉시 폐기한다.
- 대회 출품용 프로젝트. 기술 선택마다 "왜"가 있어야 한다.

---

## 2. 기술 스택

### 언어 / 프레임워크
- Java 21 + Spring Boot 4.1.1
- Spring Web, Spring Validation
- Jackson (매핑 객체 ↔ JSON)

### DB
- Redis
  - 클라이언트 정보(감싼 AES 키, authKey 해시)는 사라지면 복구 불가 → **AOF 영속성 필수**
  - 키 설계: `client:{clientId}`, `mapping:{clientId}:{chatId}` (Hash 자료형)
  - 매핑은 **생성 시점 기준 TTL 7일** (사용 시 연장은 하지 않음)

### 암호화
> JDK 내장 (`javax.crypto`, `java.security`, `java.util`), 외부 라이브러리 금지
>
| 기술 | 용도 |
|---|---|
| AES-256-GCM (`AES/GCM/NoPadding`) | 매핑 암·복호화 |
| `SecureRandom` | IV 생성 (12바이트). **암호화할 때마다 새로 생성** |
| 인증 태그 (128비트) | 키 불일치·위변조 검증. 복호화 실패(`AEADBadTagException`) → 403 |
| `KeyGenerator` | 클라이언트 등록 시 AES-256 키 생성 |
| **AES-KW** (`AESWrap`, RFC 3394) | KEK로 AES 키 감싸기/풀기. `Cipher.WRAP_MODE` / `UNWRAP_MODE`. IV 불필요. 32바이트 → 40바이트 |
| SHA-256 (`MessageDigest`) | authKey 해시 저장·비교. 비교는 `MessageDigest.isEqual` 사용 |
| Base64 (`java.util.Base64`) | 키·암호문·IV 인코딩 |

- PBKDF2, HKDF는 **프론트에서만** 쓴다. 서버는 완성된 KEK·authKey를 받기만 한다.
- `Cipher` 객체는 스레드 안전하지 않으므로 **요청마다 새로 생성**한다.

### 공통
- `@RestControllerAdvice`로 에러 응답 통일: `{ "status": int, "message": string }`
- 요청 로깅 필터(`OncePerRequestFilter`): `X-Auth-Key`, `X-Kek` 헤더와 요청 본문은 **로그에서 제외**
- 인증(`clientId` + `authKey` 검증)은 `HandlerInterceptor`에서 공통 처리
- 동시 요청 제어: **chatId 단위 락**. 단일 서버는 `ReentrantLock` 맵, 다중 서버는 Redis 락(`SET NX PX`)

---

## 3. 키 구조와 역할

| 이름 | 역할 | 만드는 곳 | 서버 보관 |
|---|---|---|---|
| `clientId` | 클라이언트(브라우저) 식별. 아이디 역할 | 프론트 (UUID) | 그대로 |
| `authKey` | `clientId`의 주인 증명. 비밀번호 역할. 패스프레이즈에서 유도 | 프론트 | **SHA-256 해시만** |
| KEK | AES 키를 감싸고 푸는 키. 패스프레이즈에서 유도 | 프론트 | **저장 안 함** (요청 처리 중에만 사용 후 폐기) |
| AES 키 | 매핑을 암·복호화하는 키. **클라이언트당 1개** | 서버 (등록 시) | **KEK로 감싼 형태로만** |
| `chatId` | 어느 대화의 매핑인지 구분 | 프론트 (UUID) | 매핑 키의 일부 |

- 클라이언트 식별은 `clientId`, 인증은 `hash(authKey)` 비교. 둘 다 통과해야 요청을 처리한다.
- 서버는 패스프레이즈, KEK 원본, AES 키 원본, 원문 텍스트를 **어떤 저장소에도 남기지 않는다.**

---

## 4. 저장 구조

### 클라이언트 (등록 시 1건)
```
client:{clientId}
  - authKeyHash    : SHA-256(authKey), Base64
  - wrappedAesKey  : KEK로 AES-KW 감싼 AES 키 (40바이트), Base64
  - createdAt
```

### 매핑 (clientId + chatId 당 1건, 대화의 첫 마스킹 요청 때 생성)
```
mapping:{clientId}:{chatId}   (TTL 7일)
  - iv          : 12바이트, Base64
  - ciphertext  : AES-GCM 암호문 + 인증 태그, Base64
  - updatedAt
```

### 매핑 평문 (ciphertext를 복호화한 JSON)
```json
{
  "mappings": [
    { "maskCode": "NAME1", "type": "NAME", "value": "홍길동" },
    { "maskCode": "PHONE1", "type": "PHONE", "value": "010-1111-1111" }
  ],
  "counters": { "NAME": 1, "PHONE": 1 }
}
```
- `counters`는 유형별 다음 번호 부여용 서버 내부 필드. 프론트는 `mappings`만 사용한다.
- "값 → 마스킹 코드" 역방향 조회는 저장하지 않고 복호화 후 메모리에서 만든다.

---

## 5. 공통 처리 흐름

### 매핑을 수정하는 API (마스킹 요청, 유형 추가/변경, 언마스킹)
```
1. X-Client-Id로 클라이언트 조회            → 없으면 401
2. SHA-256(X-Auth-Key) == authKeyHash 비교   → 다르면 401
3. X-Kek로 wrappedAesKey 풀기 → AES 키       → 실패하면 403
4. chatId 락 획득
5. mapping:{clientId}:{chatId} 조회
   └ 있으면 AES 키로 복호화 → 매핑 객체
   └ 없으면 빈 매핑 객체 (언마스킹은 404)
6. 작업 (API별로 다름)
7. 매핑 객체 → JSON → 새 IV로 AES-GCM 암호화
8. 저장 (신규 생성 시 TTL 7일 설정)
9. 락 해제
10. KEK, AES 키, 원문을 참조에서 제거 (저장·로깅·캐시 금지)
11. 204 No Content
```

### 조회 API (대화 전체 매핑 조회)
```
1~2. 위와 동일 (인증까지만)
3. wrappedAesKey + mapping의 iv/ciphertext를 그대로 반환 → 200
   (서버는 복호화하지 않는다. X-Kek도 받지 않는다)
```

### 클라이언트 등록
```
1. clientId 중복 확인                          → 있으면 409
2. KeyGenerator로 AES-256 키 생성
3. kek로 AES 키 감싸기(AES-KW) → wrappedAesKey
4. SHA-256(authKey) → authKeyHash
5. client:{clientId} 저장
6. kek, authKey, AES 키 원본 폐기 → 201 (body 없음)
```

---

## 6. 매핑 규칙 (서버 로직)

| 규칙 | 내용 |
|---|---|
| 같은 값 = 같은 코드 | `홍길동`이 여러 번 나와도 전부 `NAME1`. 언마스킹도 코드 단위라 전체 해제 |
| 번호는 서버가 부여 | 탐지기는 `(value, type)`만 반환. 번호는 `counters`로 서버가 매김 |
| 대화별 번호 | `chatId`마다 `NAME1`부터 시작 |
| 기존 값 재사용 | 이미 매핑된 값은 기존 코드 유지, 새 값만 다음 번호 |
| 유형 변경 시 새 번호 | `NAME1` → `ADDRESS`로 바꾸면 기존 매핑 삭제 후 `ADDRESS`의 다음 번호 부여 |
| 재탐지 허용 | 언마스킹한 값도 다음 마스킹 요청에서 다시 탐지될 수 있음 (제외 목록 없음) |
| 입력 정리 | 유형 추가/변경의 `value`는 앞뒤 trim 후 처리 |
| 원문 검증 | 마스킹 요청 시 탐지 결과 중 **원문에 실제로 존재하는 값만** 매핑에 반영 |
| 치환은 프론트 담당 | 서버는 마스킹된 텍스트를 만들지 않는다. 매핑만 관리 |
| 마스킹 코드 형식 | `유형코드(대문자) + 숫자` (예: `NAME1`). 유형 코드는 enum으로 검증 |

### 마스킹 유형 (enum, 한 곳에서만 관리)
| 코드 | 의미 |
|---|---|
| `NAME` | 이름 |
| `PHONE` | 전화번호 |
| `EMAIL` | 이메일 |
| `RRN` | 주민등록번호 |
| `CARD_ACCOUNT` | 카드번호 / 계좌번호 |
| `ADDRESS` | 주소 |
| `AMOUNT` | 금액 |
| `ETC` | 기타 |

### 탐지기 구조
```
Detector (인터페이스): detect(text, excludeTypes) → List<(value, type)>
 ├─ RegexDetector  ← 먼저 구현. 대회 시연 영상용 목업 데이터 테스트가 목적
 │                    (전화번호, 이메일, 주민등록번호, 카드번호 등 정규식 + 체크섬)
 └─ AiDetector     ← 방식 확정 후 추가
```
- 탐지 결과는 중복 제거·trim 후 반환한다.
- 긴 패턴부터 매칭한다 (숫자 패턴 겹침 방지).
- `excludeTypes`가 비어 있거나 없으면 전체 유형 탐지.
- 시연용 샘플 데이터는 `CommandLineRunner`로 초기 적재한다.

---

## 7. API 명세

**→ `docs/API.md` 참고.** 구현 전 반드시 해당 API 섹션을 읽을 것.
- (항상 읽을 필요는 없어 `@` import 하지 않음. 
  - 작업 지시 시 "docs/API.md N번"으로 지정)

공통 사항 요약:
- Base path: `/api`
- 인증 헤더: `X-Client-Id` (UUID), `X-Auth-Key` (Base64) / 수정 API는 `X-Kek` (Base64, 32바이트) 추가
- 성공: 등록 201, 수정 204 (body 없음), 조회 200
- 에러 body: `{ "status": int, "message": string }`

|  | 기능 | Method | Path |
|--|---|---|---|
| 1 | 클라이언트 등록 | POST | `/api/clients` |
| 2 | 마스킹 요청 | POST | `/api/chats/{chatId}/maskings` |
| 3 | 마스킹 유형 추가/변경 | PUT | `/api/chats/{chatId}/mappings` |
| 4 | 언마스킹 | DELETE | `/api/chats/{chatId}/mappings/{maskCode}` |
| 5 | 대화 전체 매핑 조회 | GET | `/api/chats/{chatId}/mappings`

---

## 8. 보안 규칙 (반드시 지킬 것)

1. **IV는 암호화할 때마다 `SecureRandom`으로 새로 생성한다.** 같은 AES 키 + 같은 IV 재사용은 GCM의 가장 치명적인 실수다.
2. KEK, AES 키 원본, authKey 원본, 원문 텍스트는 **요청 범위의 지역 변수로만** 다룬다. 세션·캐시·static·빈 필드·DB에 절대 담지 않는다.
3. 로깅 필터에서 `X-Auth-Key`, `X-Kek` 헤더와 요청 본문을 제외한다. 예외 메시지·스택트레이스에도 원문이 찍히지 않게 한다.
4. 키·원문은 URL(Path, Query)에 절대 넣지 않는다. Header/Body만 사용.
5. authKey 해시 비교는 `MessageDigest.isEqual` (타이밍 공격 방지).
6. 복호화 실패 시 403을 반환하되 상세 원인은 응답에 노출하지 않는다.
7. 원문(`value`)이 포함될 수 있는 응답에는 `Cache-Control: no-store`.
8. HTTPS만 허용

---

## 9. 미결정 사항 — 임의로 정하지 말 것

| 항목 | 상태                                                                                                         |
|---|------------------------------------------------------------------------------------------------------------|
| AI 탐지 방식 | 미정. 외부 LLM API는 원문이 외부로 나가 서비스 취지와 충돌 → 서버 내부 모델 또는 정규식 조합 검토 중. `Detector` 인터페이스는 유지하고 `RegexDetector`만 구현 |
| 매핑 TTL 연장 | 7일은 확정. 사용할 때마다 연장할지는 보류 (현재는 생성 시점 기준)                                                                    |
| 헤더 이름 | `X-Client-Id`, `X-Auth-Key`, `X-Kek`는 현재 명세 기준. 프론트에 확인 요청 중(일단 API 명세 대로 개발)                              |

---

## 10. 알려진 한계

- AI 탐지를 위해 **요청 처리 중에는 서버가 원문을 메모리에서 본다.** 저장 시에는 반드시 암호화하고 로그에 남기지 않는 것으로 대응한다.
- 사용자 PC 자체가 장악된 경우는 어떤 설계로도 막을 수 없다. 이 설계의 목적은 **서버 DB 유출 시 전체 사용자가 한 번에 노출되는 것을 막는 것**이다. 피해 범위를 "전원"에서 "털린 사용자 1명"으로 줄인다.
- 서버는 "저장된 매핑을 단독으로 복호화할 수 없다"까지만 주장한다. "절대 못 본다"는 아니다.

---

## 11. 코드 컨벤션

@docs/CONVENTION.md