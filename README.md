# S-MAP Server

S-MAP Server는 제조 현장의 주문, 생산계획, 자재, 라인 운영 데이터를 관리하고 AI 챗봇/분석 서버와 연동하기 위한 Spring Boot 기반 백엔드입니다.
현재 서버는 사용자 인증/권한, 대시보드, 주문 등록 및 조회, 생산계획 조회/수정, 자재 및 BOM 관리, 라인 운영 현황, AI 생산계획/리포트 연동 등을 제공합니다.

## 기술 스택

| 구분 | 사용 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Web | Spring WebMVC |
| Persistence | Spring Data JPA, JDBC Template |
| Security | Spring Security, JWT |
| Validation | Jakarta Bean Validation |
| Database | PostgreSQL, H2(test) |
| API Docs | springdoc-openapi |
| Build | Gradle |
| Container | Docker, Eclipse Temurin 21 |

## 주요 기능

### 인증 및 사용자 관리

- 이메일/비밀번호 기반 로그인
- JWT Access Token, Refresh Token 발급 및 재발급
- Refresh Token 해시 저장 및 재사용 방지
- 로그아웃 시 Refresh Token 폐기
- 관리자 사용자 등록, 목록 조회, 소프트 삭제
- Role 기반 접근 제어

주요 Role:

| Role | 설명 |
| --- | --- |
| `ADMIN` | 시스템 관리자 |
| `OPERATOR` | 작업자 |
| `EXECUTIVE` | 경영진 |
| `MANUFACTURING_MANAGER` | 생산 관리자 |

### 대시보드

- 전체 주문, 생산계획, 라인, 위험 지표 요약 조회
- 주간 생산 일정 조회
- 주문 납기 상태 조회
- 라인 가동률 조회
- 리스크 요약 및 최근 알림 조회

### 주문 관리

- 주문 목록 조회
- 주문 상세 조회
- 다음 주문번호 미리보기
- 신규 주문 등록
- 주문 등록 시 생산 가능한 라인 중 가장 빨리 종료 가능한 라인으로 생산계획 자동 생성
- 주문번호 동시 생성 충돌 방어
- 생산 시작일, 담당자, 납기일 등 입력 검증

### 생산계획

- 생산계획 목록 조회
- 생산계획 상세 조회
- 현재 생산계획 조회
- 생산계획 수정
- 계획 상태, 라인, 담당자, 일정, 자재 소요 정보 조회

### AI 생산계획 및 시뮬레이션

- FastAPI 계획 서버로 생산계획 생성 요청 전달
- 월간 분석 기반 생산계획 생성 요청
- 생성된 계획 시뮬레이션 목록 및 상세 조회
- 선택한 시뮬레이션 결과 저장
- Access Token과 Refresh Token 쿠키를 FastAPI 연동 요청에 전달

### 라인 운영

- 라인별 운영 상태 조회
- 설비별 운영 상태 조회
- 라인/주문 기반 주문 검색
- 주문별 라인 배분 현황 조회

### 자재 및 BOM

- 자재 등록, 목록 조회, 상세 조회, 수정
- 자재 재고 등록/수정
- 자재 사용량 조회
- 부족 자재 목록 조회
- BOM 등록, 목록 조회, 제품별 조회, 수정
- 자재 단위, 손실률, 재고 수량 검증

### 챗봇 및 AI 서버 연동

- Spring 서버에서 FastAPI 챗봇 서버로 질의 전달
- FastAPI 서버가 사용할 RDB Evidence 내부 API 제공
- 사용자 Role과 DB 상태 기준으로 Evidence 접근 제어
- 자재 부족 Evidence 조회
- 내부 API 토큰 기반 서버 간 인증

### 리포트

- AI 서버 기반 리포트 생성 요청
- 업무 리포트 생성 요청
- 리포트 생성 작업 상태 조회
- 리포트 목록 및 상세 조회
- 리포트 내용 수정
- 구조화 데이터 백필

## 프로젝트 구조

```text
src/main/java/s_map/server
├── domain
│   ├── chat        # 챗봇 요청, FastAPI 연동, Evidence 제공
│   ├── dashboard   # 메인 대시보드 지표 조회
│   ├── line        # 라인/설비 운영 현황, 주문 배분 조회
│   ├── material    # 자재, 재고, BOM
│   ├── order       # 주문, 주문번호, 주문 기반 생산계획 생성
│   ├── plan        # 생산계획 조회/수정, AI 계획 생성, 시뮬레이션
│   ├── report      # AI 리포트 생성, 조회, 수정
│   ├── token       # Refresh Token 저장, 검증, 정리
│   └── user        # 로그인, 관리자 사용자 관리
└── global
    ├── common      # 공통 응답, 공통 엔티티
    ├── config      # Security, CORS 설정
    ├── error       # 예외 코드, 전역 예외 처리
    └── security    # JWT, 인증 필터, 내부 토큰 검증
```

## 실행 방법

### 1. 사전 준비

- Java 21
- PostgreSQL
- Gradle Wrapper 사용 권장

로컬 기본 DB 설정은 다음 값을 사용합니다.

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/smap
SPRING_DATASOURCE_USERNAME=smap
DB_PASSWORD=<local-db-password>
JWT_SECRET=<jwt-secret>
```

주요 환경 변수:

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 실행 프로필 | `local` |
| `SPRING_DATASOURCE_URL` | local 프로필 DB URL | `jdbc:postgresql://localhost:5432/smap` |
| `SPRING_DATASOURCE_USERNAME` | local 프로필 DB 사용자 | `smap` |
| `DB_URL` | prod 프로필 DB URL | 없음 |
| `DB_USERNAME` | prod 프로필 DB 사용자 | 없음 |
| `DB_PASSWORD` | DB 비밀번호 | 없음 |
| `JWT_SECRET` | JWT 서명 키 | 없음 |
| `APP_CORS_ALLOWED_ORIGINS` | prod CORS 허용 Origin 목록 | 없음 |
| `FASTAPI_BASE_URL` | FastAPI 서버 Base URL | `http://fastapi-service:8000` |
| `AI_FASTAPI_PLANNING_GENERATE_PATH` | FastAPI 생산계획 생성 경로 | `/ai/api/v1/planning` |
| `CHAT_EVIDENCE_INTERNAL_TOKEN` | FastAPI가 Spring Evidence API를 호출할 때 사용하는 내부 토큰 | 없음 |
| `CHAT_ANSWER_INTERNAL_TOKEN` | Spring이 FastAPI 챗봇 응답 API를 호출할 때 사용하는 내부 토큰 | 없음 |
| `SPRING_MAIL_HOST` | SMTP 서버 호스트 | 없음 |
| `SPRING_MAIL_PORT` | SMTP 서버 포트 | 없음 |
| `SPRING_MAIL_USERNAME` | SMTP 로그인 계정 | 없음 |
| `SPRING_MAIL_PASSWORD` | SMTP 로그인 비밀번호 또는 앱 비밀번호 | 없음 |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | SMTP 인증 사용 여부 | 없음 |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | SMTP STARTTLS 사용 여부 | 없음 |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_CONNECTIONTIMEOUT` | SMTP 연결 타임아웃(ms) | 없음 |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT` | SMTP 응답 타임아웃(ms) | 없음 |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_WRITETIMEOUT` | SMTP 쓰기 타임아웃(ms) | 없음 |
| `REPORT_MAIL_FROM` | 리포트 메일 발신자 주소 | `no-reply@smap.local` |

### 2. 로컬 실행

```bash
export DB_PASSWORD=<local-db-password>
export JWT_SECRET=<jwt-secret>
./gradlew bootRun
```

기본 프로필은 `local`이며 서버 포트는 `8080`입니다.

리포트 메일 발송을 로컬에서 테스트하려면 SMTP 환경 변수를 함께 지정합니다. Gmail SMTP를 사용하는 경우 `SPRING_MAIL_PASSWORD`에는 Google 계정 비밀번호가 아니라 앱 비밀번호를 사용합니다.

```bash
export SPRING_MAIL_HOST=smtp.gmail.com
export SPRING_MAIL_PORT=587
export SPRING_MAIL_USERNAME=<gmail-address>
export SPRING_MAIL_PASSWORD=<gmail-app-password-without-spaces>
export SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
export SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
export SPRING_MAIL_PROPERTIES_MAIL_SMTP_CONNECTIONTIMEOUT=5000
export SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT=3000
export SPRING_MAIL_PROPERTIES_MAIL_SMTP_WRITETIMEOUT=5000
export REPORT_MAIL_FROM=<gmail-address>
```

Kubernetes 배포에서는 SMTP host, port, timeout, `REPORT_MAIL_FROM`처럼 노출되어도 되는 값은 `backend-config` ConfigMap에 두고, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`는 `backend-mail-secret` Secret으로 주입합니다. 앱 비밀번호는 Git에 커밋하지 않습니다.

### 3. 테스트 실행

```bash
./gradlew test
```

테스트 프로필은 H2를 PostgreSQL 호환 모드로 사용합니다. 별도 PostgreSQL 연결 없이 실행됩니다.

### 4. API 문서 및 헬스 체크

로컬 실행 후 다음 URL을 사용할 수 있습니다.

| 구분 | URL |
| --- | --- |
| Swagger UI | `http://localhost:8080/api/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api/v3/api-docs` |
| Health Check | `http://localhost:8080/actuator/health` |

### 5. Docker 이미지 빌드

```bash
docker build -t s-map-server:local .
```

컨테이너 실행 예시:

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/smap \
  -e DB_USERNAME=smap \
  -e DB_PASSWORD=<db-password> \
  -e JWT_SECRET=<jwt-secret> \
  -e APP_CORS_ALLOWED_ORIGINS=http://localhost:3000 \
  -e CHAT_EVIDENCE_INTERNAL_TOKEN=<internal-token> \
  -e CHAT_ANSWER_INTERNAL_TOKEN=<internal-token> \
  -e SPRING_MAIL_HOST=smtp.gmail.com \
  -e SPRING_MAIL_PORT=587 \
  -e SPRING_MAIL_USERNAME=<gmail-address> \
  -e SPRING_MAIL_PASSWORD=<gmail-app-password-without-spaces> \
  -e SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true \
  -e SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true \
  -e SPRING_MAIL_PROPERTIES_MAIL_SMTP_CONNECTIONTIMEOUT=5000 \
  -e SPRING_MAIL_PROPERTIES_MAIL_SMTP_TIMEOUT=3000 \
  -e SPRING_MAIL_PROPERTIES_MAIL_SMTP_WRITETIMEOUT=5000 \
  -e REPORT_MAIL_FROM=<gmail-address> \
  s-map-server:local
```

## 인증 및 권한

- 로그인과 토큰 재발급을 제외한 API는 기본적으로 `Authorization: Bearer <accessToken>` 헤더가 필요합니다.
- `/api/auth/login`, `/api/token/refresh`, Swagger, Health Check는 인증 없이 접근할 수 있습니다.
- `/api/admin/**`는 `ADMIN` 권한만 접근할 수 있습니다.
- 자재/BOM 생성 및 수정은 `ADMIN`, `EXECUTIVE`, `MANUFACTURING_MANAGER` 권한이 필요합니다.
- 주문 생성은 `MANUFACTURING_MANAGER` 권한이 필요합니다.
- AI 생산계획 생성, 시뮬레이션 저장, 생산계획 수정은 `ADMIN`, `EXECUTIVE`, `MANUFACTURING_MANAGER` 권한이 필요합니다.
- FastAPI 내부 연동 API인 `/internal/chat/evidence`는 `X-Internal-Token` 헤더로 `CHAT_EVIDENCE_INTERNAL_TOKEN` 값을 전달해야 합니다.

## 주요 API 그룹

| 구분 | Method / Path | 설명 |
| --- | --- | --- |
| 인증 | `POST /api/auth/login` | 로그인 |
| 인증 | `GET /api/auth/me` | 현재 사용자 조회 |
| 인증 | `POST /api/auth/logout` | 로그아웃 |
| 토큰 | `POST /api/token/refresh` | Access Token 재발급 |
| 관리자 | `/api/admin/**` | 관리자 사용자 및 대시보드 관리 |
| 대시보드 | `GET /api/dashboard/**` | 요약, 주간 일정, 납기, 라인 가동률, 리스크, 알림 조회 |
| 주문 | `GET /api/orders` | 주문 목록 조회 |
| 주문 | `GET /api/orders/{orderId}` | 주문 상세 조회 |
| 주문 | `GET /api/orders/next-order-no` | 다음 주문번호 미리보기 |
| 주문 | `POST /api/orders` | 주문 등록 및 생산계획 자동 생성 |
| 생산계획 | `GET /api/plans` | 생산계획 목록 조회 |
| 생산계획 | `GET /api/plans/{planId}` | 생산계획 상세 조회 |
| 생산계획 | `GET /api/plans/current` | 현재 생산계획 조회 |
| 생산계획 | `PATCH /api/plans/{planId}` | 생산계획 수정 |
| 생산계획 | `PATCH /api/plans/{planId}/schedule` | 생산계획 일정 수정 |
| AI 계획 | `POST /api/plans/ai/generate` | AI 생산계획 생성 요청 |
| AI 계획 | `POST /api/plans/ai/monthly-analysis` | 월간 분석 기반 생산계획 생성 요청 |
| 시뮬레이션 | `GET /api/plans/simulations` | 계획 시뮬레이션 목록 조회 |
| 시뮬레이션 | `GET /api/plans/simulations/{simulationId}/details` | 계획 시뮬레이션 상세 조회 |
| 시뮬레이션 | `POST /api/plans/simulations/selected` | 선택 시뮬레이션 저장 |
| 라인 | `GET /api/lines/**` | 라인/설비 운영 상태 및 주문 배분 조회 |
| 자재 | `GET /api/materials/**` | 자재, 재고, 사용량, 부족 자재 조회 |
| 자재 | `POST /api/materials` | 자재 등록 |
| 자재 | `PUT /api/materials/{materialId}` | 자재 수정 |
| 자재 | `PUT /api/materials/{materialId}/inventory` | 자재 재고 수정 |
| BOM | `GET /api/materials/boms/**` | BOM 목록 및 제품별 BOM 조회 |
| BOM | `POST /api/materials/boms` | BOM 등록 |
| BOM | `PUT /api/materials/boms/{bomId}` | BOM 수정 |
| 챗봇 | `POST /api/chat/answer` | FastAPI 챗봇 답변 요청 |
| 챗봇 내부 | `POST /internal/chat/evidence` | FastAPI용 RDB Evidence 조회 |
| 리포트 | `POST /api/reports/generate` | AI 리포트 생성 시작 |
| 리포트 | `POST /api/reports/business` | 업무 리포트 생성 |
| 리포트 | `GET /api/reports/jobs/{reportJobId}` | 리포트 생성 작업 조회 |
| 리포트 | `GET /api/reports` | 리포트 목록 조회 |
| 리포트 | `GET /api/reports/{reportId}` | 리포트 상세 조회 |
| 리포트 | `GET /api/reports/{reportId}/pdf` | 리포트 PDF 다운로드 |
| 리포트 | `POST /api/reports/{reportId}/mail` | 리포트 메일 발송 |
| 리포트 | `PATCH /api/reports/{reportId}` | 리포트 수정 |

## 응답 규격

모든 API는 `BaseResponse<T>` 형태로 응답합니다.

성공:

```json
{
  "success": true,
  "code": "COMMON200",
  "message": "요청 성공",
  "data": {}
}
```

## 개발 컨벤션

### 패키지

- 도메인별로 `controller`, `dto`, `entity`, `repository`, `service` 패키지를 분리합니다.
- 공통 설정, 예외, 보안 코드는 `global` 하위에 둡니다.
- 새로운 업무 도메인은 `domain/<domain-name>` 하위에 같은 구조로 추가합니다.

### DTO

- 요청 DTO는 `dto/req`, 응답 DTO는 `dto/res`에 둡니다.
- 외부 입력값 형식 검증은 요청 DTO의 Jakarta Validation으로 처리합니다.
- 두 필드 이상이 함께 만족해야 하는 복합 조건은 `@AssertTrue` 메서드 또는 서비스 검증으로 처리합니다.
- 응답 DTO는 엔티티를 직접 노출하지 않고 정적 팩토리 메서드로 변환합니다.

### Service

- 트랜잭션 경계는 서비스 계층에 둡니다.
- 기본 조회 서비스는 `@Transactional(readOnly = true)`를 사용합니다.
- 생성/수정/삭제 메서드는 `@Transactional`을 명시합니다.
- 업무 규칙 위반은 `CustomException`과 `ErrorCode`로 표현합니다.

### Repository

- 단순 CRUD는 Spring Data JPA Repository를 사용합니다.
- 복잡한 목록/집계/상태 계산은 명시 쿼리 또는 JDBC Template을 사용합니다.
- Native Query 사용 시 응답 Projection을 명확히 분리합니다.

### Entity

- 외부에서 직접 생성자를 호출하지 않도록 기본 생성자는 `PROTECTED`로 둡니다.
- 생성 의도가 있는 경우 정적 팩토리 메서드 또는 Builder를 사용합니다.
- 상태 변경은 엔티티 메서드로 캡슐화합니다.

### Error Code

- 공통 예외 코드는 `global/error/ErrorCode.java`에서 관리합니다.
- HTTP 상태와 서비스 에러 코드를 함께 정의합니다.
- 클라이언트가 분기해야 하는 오류는 별도 세부 코드를 추가합니다.

### 테스트

- 통합 테스트는 `@SpringBootTest`와 `@AutoConfigureMockMvc`를 사용합니다.
- 단위 테스트는 Mockito 기반으로 서비스 로직을 검증합니다.
- 보안/권한/검증 실패처럼 회귀 위험이 큰 흐름은 통합 테스트로 고정합니다.
- 테스트는 H2 PostgreSQL 호환 모드에서 실행됩니다.

### 커밋 메시지

Conventional Commits 형식을 권장합니다.

```text
feat: 주문 등록 기능 추가
fix: 로그아웃 Refresh Token 멱등 처리
docs: README 프로젝트 설명 추가
test: 주문 등록 검증 테스트 추가
refactor: 자재 재고 상태 계산 정리
chore: Gradle 설정 정리
```

## 작업 전 체크리스트

1. 변경 범위를 도메인 단위로 좁힙니다.
2. 기존 테스트가 있는지 먼저 확인합니다.
3. 입력 검증은 DTO, 업무 규칙은 Service, 데이터 무결성은 Entity/DB 제약에 둡니다.
4. 권한이 필요한 API는 SecurityConfig와 통합 테스트를 함께 확인합니다.
5. 변경 후 `./gradlew test`를 실행합니다.
