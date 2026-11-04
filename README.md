# Backend

개인정보 마스킹 API 서버. Java 21, Spring Boot 4.1.1, Redis를 사용합니다.
등록·마스킹·유형 변경·언마스킹·암호문 조회 API가 구현되어 있으며,
현재 탐지기는 정규식과 시연용 고정값을 사용합니다. API 계약은 [docs/API.md](docs/API.md)를 참고하세요.

## 환경 설정과 실행

입력할 값은 [한 페이지 PDF 안내](output/pdf/frontend-env-guide.pdf)에서도 확인할 수 있습니다.

프로젝트 루트에서 실행합니다. `application.yaml`이 루트의 `.env`를 자동으로 읽으며,
같은 이름의 운영체제 환경변수가 있으면 환경변수가 우선합니다.
`.env`는 Java properties 형식이므로 값에 따옴표나 `export`를 넣지 마세요.

```powershell
Copy-Item .env.example .env
```

기본 로컬 환경에서는 **두 값만 입력**하면 됩니다.

| 필수 값 | 넣을 내용 |
|---|---|
| `CORS_ALLOWED_ORIGINS` | 실제 프론트 origin 또는 `chrome-extension://확장ID` |
| `SERVER_SSL_KEY_STORE_PASSWORD` | 아래에서 생성한 인증서의 비밀번호 |

Redis는 기본 `localhost:6379`, 비밀번호 없음입니다. 다를 때만 `.env.example`의
`REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` 주석을 풀어 변경하세요.
Redis ACL/TLS 등 추가 설정이 필요하면 Spring 기본 환경변수
`SPRING_DATA_REDIS_USERNAME`, `SPRING_DATA_REDIS_SSL_ENABLED`를 실행 환경에 지정할 수 있습니다.

`.env`에서 `CORS_ALLOWED_ORIGINS`를 실제 프론트 주소로 바꿉니다.
여러 주소는 쉼표로 구분하며 경로와 마지막 `/`는 제외합니다.
예: `https://localhost:5173,chrome-extension://실제확장ID`.
미설정 시 다른 origin의 브라우저 요청을 허용하지 않습니다.
쿠키 인증은 사용하지 않으므로 fetch의 `credentials: 'include'`는 지정하지 않습니다.

### HTTPS 인증서

기본 API 주소는 `https://localhost:8443/api`입니다. HTTPS 전용 지침에 따라
기본적으로 TLS를 켭니다. 로컬 인증서는 다음과 같이 만들 수 있습니다.
비밀번호 프롬프트에 입력한 값을 `.env`의 `SERVER_SSL_KEY_STORE_PASSWORD`에도 설정하세요.

```powershell
New-Item -ItemType Directory -Force certs
keytool -genkeypair -alias local -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore certs/local.p12 -validity 365 -dname "CN=localhost" -ext "SAN=dns:localhost,ip:127.0.0.1"
```

자체 서명 인증서는 브라우저/운영체제에서 신뢰하도록 등록해야 브라우저 요청이 정상 동작합니다.
배포 시에는 신뢰된 인증서를 사용하세요. 다른 PC에서 접속하게 하려면
`SERVER_ADDRESS`와 인증서의 도메인, 프론트 API 주소를 함께 맞춰야 합니다.
HTTPS를 처리하는 리버스 프록시 뒤에서만 `SERVER_SSL_ENABLED=false`를 사용할 수 있으며,
이 경우 백엔드 HTTP 포트는 외부에 공개하지 마세요.

### Redis

기존 Redis를 쓰면 `.env`의 접속 정보를 맞추고 **Redis 서버에서 AOF를 활성화**하세요.
Spring의 YAML로 AOF를 활성화할 수는 없습니다. 로컬 Docker 실행 예시는 다음과 같습니다.

```powershell
docker run -d --name eveil-redis -p 127.0.0.1:6379:6379 -v eveil-redis-data:/data redis:7 redis-server --appendonly yes --appendfsync everysec
docker exec eveil-redis redis-cli CONFIG GET appendonly
.\gradlew.bat bootRun
```

AOF 조회 결과가 `yes`인지 확인하세요. Docker 볼륨에는 클라이언트 키 정보가 저장되므로
삭제하면 복구할 수 없습니다. 매핑 TTL은 생성 시점 기준 7일로 유지됩니다.

## 프론트 연결

프론트에는 API 주소만 공개 설정으로 전달합니다. 예를 들어 Vite 프로젝트라면:

```dotenv
VITE_API_BASE_URL=https://localhost:8443/api
```

프론트 프레임워크에 맞는 환경변수 이름을 사용하세요. Redis 비밀번호, 인증서 비밀번호,
사용자의 `authKey`와 KEK는 프론트 공개 환경변수에 넣지 않습니다.
`clientId`, `authKey`, KEK는 API 명세대로 클라이언트가 생성하여 요청마다 전달합니다.

- 등록: `POST /clients`에 `clientId`, `authKey`, `kek` JSON → 201.
- 수정: `X-Client-Id`, `X-Auth-Key`, `X-Kek` 헤더 → 204, 응답 본문 없음.
- 조회: `GET /chats/{chatId}/mappings`에 `X-Client-Id`, `X-Auth-Key`만 전달 → 암호문 반환.
- 프론트에서 응답을 복호화하고 텍스트를 치환합니다. 수정 응답에 `response.json()`을 호출하지 마세요.

설정 파일의 비밀번호는 `.env`에만 두며 `.env`와 `certs/`는 Git에서 제외합니다.
