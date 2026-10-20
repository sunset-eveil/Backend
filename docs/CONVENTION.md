# 코드 컨벤션

> `CLAUDE.md`에서 `@docs/CONVENTION.md`로 import되어 **항상 로드**된다. 모든 코드 작성 시 적용.

## 패키지 구조
```
com.sunset.eveil
├── feature/                 ← 기능(도메인)별 패키지
│   ├── client/              클라이언트 등록 (홍길동)
│   ├── masking/             마스킹 요청 (홍길동)
│   └── mapping/             매핑 관리 — 유형 변경·언마스킹·조회 (홍길다)
└── global/                  ← 두 개 이상의 feature가 쓰는 공통 부품
    ├── auth/                ClientAuthInterceptor, AuthenticatedClient, ClientAuthArgumentResolver
    ├── crypto/              AesGcmCipher, KeyWrapper, AesKeyGenerator, Sha256Hasher, CryptoException
    ├── lock/                ChatLockManager
    ├── error/               BusinessException, ErrorResponse, GlobalExceptionHandler
    ├── logging/             SensitiveHeaderFilter
    └── config/              RedisConfig, WebConfig (@Configuration 클래스만)
```

각 feature 하위는 다음 5개로 고정한다.
```
feature/{name}/
├── presentation/            Controller, dto/ (Request·Response record)
├── application/             유스케이스 Service
├── domain/                  도메인 객체, 값 객체, Repository 인터페이스, enum
├── infrastructure/          Repository 구현(Redis), 외부 기술 어댑터(RegexDetector 등)
└── exception/               해당 feature 전용 예외
```

## 의존 방향
```
presentation → application → domain ← infrastructure
```
- 화살표 방향으로만 호출한다. 역방향 금지.
- `domain` 패키지는 **Spring·Redis import 금지**. Lombok, Jackson 어노테이션은 허용 (직렬화·보일러플레이트 목적).
- feature 간 직접 참조 금지. 다른 feature의 기능이 필요하면 그 feature의 `application` Service를 통해서만 호출한다. 다른 feature의 Repository 직접 호출 금지.

## global 등록 기준 — 둘 다 "예"일 때만
1. 두 개 이상의 feature가 사용하는가?
2. feature 내용을 몰라도 역할을 설명할 수 있는가?

하나라도 "아니오"면 해당 feature 안에 둔다. (예: `RegexDetector`는 masking만 쓰므로 `feature/masking/infrastructure`)

## 클래스 규칙
| 대상 | 규칙 | 예 |
|---|---|---|
| Service | 유스케이스당 1개, `동사+대상+Service`. **public 메서드는 `execute` 하나**. private 메서드는 최소화하고, 쪼개고 싶으면 도메인 객체로 옮긴다 | `RegisterClientService.execute(...)`, `MaskTextService.execute(...)` |
| Controller | feature당 1개. 요청 검증·Service 호출·상태 코드 결정만. 로직 금지 | `MaskingController` |
| 도메인 객체 | 비즈니스 규칙(번호 매기기, 코드 검증, 상태 변경)을 메서드로 가짐 | `MappingTable.assign()`, `.remove()`, `.changeType()` |
| 값 객체 | record + 생성자 검증 | `MaskCode`, `DetectedEntity` |
| Repository | `domain`에 인터페이스, `infrastructure`에 `기술+도메인+Repository` 구현 | `MappingRepository` → `RedisMappingRepository` |
| DTO | record. 요청 `~Request`, 응답 `~Response`. Controller 밖으로 넘기지 않음 | `UpdateMappingTypeRequest` |
| 예외 | `BusinessException(HttpStatus)` 상속, feature의 `exception` 패키지 | `MappingNotFoundException` |
| 암호화 부품 | 빈으로 주입. static 유틸 금지 | `AesGcmCipher` |

- Service의 `execute`는 "꺼내서 → 도메인에 시키고 → 저장" 흐름만 가진다. 메서드가 길어지면 private으로 쪼개지 말고 도메인으로 옮길 로직이 있다는 신호로 본다.
- Lombok은 `@RequiredArgsConstructor`, `@Getter`만. `@Data`, `@Setter` 금지.

## record 규칙
- 필드 이름만 보고 **무엇인지, 어떤 형식인지** 알 수 있어야 한다. 축약 금지 (`val` ✗ → `value` ✓, `wk` ✗ → `wrappedAesKey` ✓).
- 인코딩·단위가 있으면 이름에 드러낸다 (`ivBase64`보다는 타입으로: `String iv` + 주석 "Base64" 또는 값 객체).
- 필드 순서는 **식별자 → 핵심 값 → 부가 정보** 순으로 통일한다.
- 필드가 4개를 넘으면 묶을 수 있는 값 객체가 있는지 먼저 본다.
- 생성자 검증(`null`, 빈 값, 형식)은 compact constructor에서 한다.
- 작성 후 **각 필드를 읽어 보고 모호한 이름이 없는지 다시 점검**한다.

## 금지 이름
`util`, `utils`, `common`, `helper`, `misc`, `detail`, `base`, `core`, `global/dto`, `global/service`
— 넣을 곳이 없으면 util을 만들지 말고 **새 관심사 패키지**(예: `global/time/`)를 만든다.
