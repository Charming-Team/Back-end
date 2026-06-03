# S-MAP Server

S-MAP Server는 제조 현장의 주문, 생산계획, 자재, 라인 운영 데이터를 관리하고 AI 챗봇/분석 서버와 연동하기 위한 Spring Boot 기반 백엔드입니다.
현재 서버는 사용자 인증/권한, 주문 등록 및 조회, 생산계획 조회/수정, 자재 및 BOM 관리 등을 제공합니다.

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

## 프로젝트 구조

```text
src/main/java/s_map/server
├── domain
│   ├── chat        # 챗봇 요청, FastAPI 연동, Evidence 제공
│   ├── material    # 자재, 재고, BOM
│   ├── order       # 주문, 주문번호, 주문 기반 생산계획 생성
│   ├── plan        # 생산계획 조회/수정
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

### 2. 로컬 실행

```bash
export DB_PASSWORD=<local-db-password>
export JWT_SECRET=<jwt-secret>
./gradlew bootRun
```

기본 프로필은 `local`이며 서버 포트는 `8080`입니다.

### 3. 테스트 실행

```bash
./gradlew test
```

테스트 프로필은 H2를 PostgreSQL 호환 모드로 사용합니다. 별도 PostgreSQL 연결 없이 실행됩니다.

### 4. Docker 이미지 빌드

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
  s-map-server:local
```

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
