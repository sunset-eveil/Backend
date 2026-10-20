> 개인정보 마스킹 크롬 확장 — 백엔드 API 명세.
> 키 구조·저장 구조·처리 흐름·보안 규칙은 루트의 `CLAUDE.md`를 따른다.

# API 명세

## 공통
- Base path: `/api`
- 인증 헤더: `X-Client-Id` (UUID), `X-Auth-Key` (Base64)
- 수정 API 추가 헤더: `X-Kek` (Base64, 32바이트)
- 에러 body: `{ "status": int, "message": string }`
- 마스킹 유형은 프론트·서버 공통 enum (`CLAUDE.md` 6번 참고)

| # | 기능 | Method | Path | 담당 | 성공 |
|---|---|---|---|---|---|
| 1 | 클라이언트 등록 | POST | `/api/clients` | 권수현 | 201 |
| 2 | 마스킹 요청 | POST | `/api/chats/{chatId}/maskings` | 권수현 | 204 |
| 3 | 마스킹 유형 추가/변경 | PUT | `/api/chats/{chatId}/mappings` | 준윤 | 204 |
| 4 | 언마스킹 | DELETE | `/api/chats/{chatId}/mappings/{maskCode}` | 준윤 | 204 |
| 5 | 대화 전체 매핑 조회 | GET | `/api/chats/{chatId}/mappings` | 준윤 | 200 |

---

## 1. 클라이언트 등록 — `POST /api/clients`

확장 설치 시 1회 호출. 서버가 AES 키를 생성해 KEK로 감싸 저장하고, authKey는 해시로만 저장한다. 매핑은 이 시점에 만들지 않는다.

**Header**: `Content-Type: application/json`

**Body**
```json
{
  "clientId": "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
  "authKey": "Base64",
  "kek": "Base64"
}
```
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| clientId | String (UUID) | O | 클라이언트가 생성한 식별자 |
| authKey | String | O | 패스프레이즈에서 유도한 인증 키 (Base64) |
| kek | String | O | 패스프레이즈에서 유도한 키 암호화 키 (Base64, 32바이트) |

**Response**
- `201 Created` — body 없음
- `400` — 필드 누락/빈 값, clientId UUID 형식 오류, authKey·kek 형식·길이 오류 → `"요청 형식이 올바르지 않습니다."`
- `409` — 이미 등록된 clientId → `"이미 등록된 클라이언트입니다."`
- `500` — 키 생성·저장 오류

---

## 2. 마스킹 요청 — `POST /api/chats/{chatId}/maskings`

입력 텍스트에서 개인정보를 탐지해 대화의 매핑에 반영하고 암호화 저장한다. 대화의 첫 요청이면 매핑을 새로 생성한다. 결과는 응답에 담지 않고 GET으로 조회한다.

**Header**: `Content-Type`, `X-Client-Id`, `X-Auth-Key`, `X-Kek`

**Path**: `chatId` (UUID)

**Body**
```json
{
  "text": "안녕하세요 권수현입니다. 권수현의 번호는 010-1111-1111입니다.",
  "excludeTypes": ["ADDRESS"]
}
```
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| text | String | O | 마스킹할 원문 (공백만 있는 값 불가) |
| excludeTypes | String[] | X | 탐지에서 제외할 유형. 비어 있거나 없으면 전체 탐지 |

**Response**
- `204 No Content`
- `400` — text 비어 있음/공백만, excludeTypes에 정의되지 않은 유형, 헤더 형식 오류 → `"마스킹할 텍스트가 비어 있습니다."` 등
- `401` — 헤더 누락, 미등록 클라이언트, authKey 불일치 → `"클라이언트 인증에 실패했습니다."`
- `403` — X-Kek로 AES 키를 풀 수 없음 → `"매핑 정보를 열 수 없습니다."`
- `500` — 탐지·암호화 오류

---

## 3. 마스킹 유형 추가/변경 — `PUT /api/chats/{chatId}/mappings`

사용자가 선택한 값에 마스킹 유형을 지정·변경한다. AI를 거치지 않고 서버 로직으로 처리. 대화에 매핑이 없으면 새로 생성한다.

**Header**: `Content-Type`, `X-Client-Id`, `X-Auth-Key`, `X-Kek`

**Path**: `chatId` (UUID)

**Body**
```json
{ "value": "권수현", "type": "ADDRESS" }
```
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| value | String | O | 지정할 값 (앞뒤 trim 후 처리, 공백만 있는 값 불가) |
| type | String | O | 지정할 마스킹 유형 |

**Response**
- `204 No Content`
- `400` — value 비어 있음/공백만, type 미정의, 헤더 형식 오류 → `"지정할 값이 비어 있습니다."` 등
- `401`, `403`, `500` — 2번과 동일

---

## 4. 언마스킹 — `DELETE /api/chats/{chatId}/mappings/{maskCode}`

지정한 마스킹 코드를 매핑에서 삭제해 해당 값의 마스킹을 해제한다. 코드 단위 삭제이므로 같은 값 전체가 함께 해제된다.

**Header**: `X-Client-Id`, `X-Auth-Key`, `X-Kek` (body 없음)

**Path**: `chatId` (UUID), `maskCode` (예: `NAME1`)

**Response**
- `204 No Content`
- `400` — maskCode 형식 오류(유형+번호 형식 아님), 헤더 형식 오류 → `"마스킹 코드 형식이 올바르지 않습니다."`
- `401`, `403` — 2번과 동일
- `404` — 해당 대화의 매핑 없음, 매핑에 해당 maskCode 없음 → `"해당 마스킹 정보를 찾을 수 없습니다."`
- `500` — 암호화 오류

---

## 5. 대화 전체 매핑 조회 — `GET /api/chats/{chatId}/mappings`

암호화된 매핑을 그대로 반환한다. 서버는 복호화하지 않으며 X-Kek를 받지 않는다. 클라이언트가 KEK로 wrappedAesKey를 풀고 그 AES 키로 ciphertext를 복호화한다.

**Header**: `X-Client-Id`, `X-Auth-Key`

**Path**: `chatId` (UUID)

**Response `200 Ok`**
```json
{
  "wrappedAesKey": "KEK로 감싼 AES 키 (Base64)",
  "iv": "매핑 암호화에 사용한 IV (Base64)",
  "ciphertext": "암호화된 매핑 + 인증 태그 (Base64)"
}
```
| 필드 | 타입 | 설명 |
|---|---|---|
| wrappedAesKey | String | KEK로 AES-KW 감싼 클라이언트의 AES 키 (40바이트) |
| iv | String | 매핑 암호화에 사용한 IV (12바이트) |
| ciphertext | String | 암호화된 매핑 (인증 태그 포함) |

- 응답 헤더에 `Cache-Control: no-store`
- `400` — 헤더 형식 오류 → `"요청 형식이 올바르지 않습니다."`
- `401` — 2번과 동일
- `404` — 해당 대화의 매핑 없음 → `"해당 대화의 매핑 정보가 없습니다."` (프론트는 빈 매핑으로 처리)
- `500` — 조회 오류

---
