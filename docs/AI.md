# 로컬 AI 연결

AI 코드는 `.local-ai/`, 모델은 `.local-ai/models/Eveil-AI/`에 배치한다.
두 경로 모두 백엔드 Git 추적에서 제외한다. AI 저장소 갱신은 해당 폴더에서 `git pull --ff-only`로 진행한다.

AI 저장소 안내 기준 Python 3.10 환경에서 `.local-ai` 폴더를 기준으로 실행한다.

```powershell
py -3.10 -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
.\.venv\Scripts\python.exe server.py
```

서버의 `완료` 출력 후 백엔드를 실행한다. 모델 경로는 실행 폴더 기준이다.
기본 탐지기는 AI이며 `POST /api/chats/{chatId}/maskings` 경로는 유지한다.

- `MASKING_DETECTOR=ai`: AI만 사용 (기본값)
- `MASKING_DETECTOR=demo`: 기존 FixedValueDetector와 RegexDetector 조합 사용
- `AI_BASE_URL`: 기본 `http://127.0.0.1:8000`
- `AI_TIMEOUT`: 연결·응답 대기 제한. 임시 기본값 `30s`, 실제 장비 측정 후 조정

AI 입력은 특수 토큰 포함 256토큰, HTTP 본문은 65,536바이트 이하이다.
백엔드는 원문을 변경하지 않고 전송하며 응답의 코드 포인트 위치를 Java 위치로 변환한다.
AI 오류·통신 실패는 빈 탐지 결과로 대체하지 않는다. 현재 공개 API 명세대로 500 오류로 처리한다.
AI 오류 본문은 사용자에게 전달하지 않는다. 별도 413·503 공개 응답이 필요하면 API 명세를 먼저 변경한다.

같은 PC에서 실행하는 루프백 전용 시연 구성이다. 외부 장비·컨테이너 배포에는 별도 주소와 보안 구성이 필요하다.
