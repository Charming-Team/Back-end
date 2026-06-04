package s_map.server.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "400", "잘못된 요청입니다."),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "400-001", "요청 값 검증에 실패했습니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "400-002", "요청 본문을 읽을 수 없습니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "400-003", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),
    INVALID_INVENTORY_QUANTITY(HttpStatus.BAD_REQUEST, "400-101", "예약 재고 수량은 현재 재고 수량보다 클 수 없습니다."),
    INVALID_INVENTORY_OPERATION_QUANTITY(HttpStatus.BAD_REQUEST, "400-102", "재고 처리 수량은 0보다 커야 합니다."),
    INSUFFICIENT_AVAILABLE_INVENTORY(HttpStatus.BAD_REQUEST, "400-103", "예약 가능한 재고 수량이 부족합니다."),
    INVALID_INVENTORY_RELEASE_QUANTITY(HttpStatus.BAD_REQUEST, "400-104", "해제할 예약 수량은 현재 예약 재고보다 클 수 없습니다."),
    INVALID_ORDER_DATE(HttpStatus.BAD_REQUEST, "400-301", "주문 일정 조건이 올바르지 않습니다."),
    INVALID_ORDER_OPERATOR(HttpStatus.BAD_REQUEST, "400-302", "생산 담당자 정보가 올바르지 않습니다."),
    ORDER_SCHEDULE_EXCEEDS_DUE_DATE(HttpStatus.BAD_REQUEST, "400-303", "생산계획 종료일은 납기일을 초과할 수 없습니다."),
    INVALID_REPORT_REQUEST(HttpStatus.BAD_REQUEST, "400-401", "보고서 요청 값이 올바르지 않습니다."),
    INVALID_REPORT_PERIOD(HttpStatus.BAD_REQUEST, "400-402", "보고서 기간 조건이 올바르지 않습니다."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "401-001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "401-002", "만료된 토큰입니다."),
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "401-003", "아이디 또는 비밀번호가 올바르지 않습니다."),
    INVALID_INTERNAL_TOKEN(HttpStatus.UNAUTHORIZED, "401-004", "내부 API 토큰이 올바르지 않습니다."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "403", "접근 권한이 없습니다."),
    ADMIN_AUTH_REQUIRED(HttpStatus.FORBIDDEN, "403-001", "관리자만 로그인할 수 있습니다."),
    INACTIVE_ACCOUNT(HttpStatus.FORBIDDEN, "403-002", "사용할 수 없는 계정입니다."),
    USER_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "403-201", "사용자 삭제 권한이 없습니다."),
    SELF_DELETE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "403-202", "본인 계정은 삭제할 수 없습니다."),
    ADMIN_DELETE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "403-203", "시스템 관리자 계정은 삭제할 수 없습니다."),
    REPORT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "403-301", "보고서 접근 권한이 없습니다."),

    // 404 Not Found
    NOT_FOUND(HttpStatus.NOT_FOUND, "404", "요청한 리소스를 찾을 수 없습니다."),
    MATERIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "404-101", "자재 정보를 찾을 수 없습니다."),
    MATERIAL_INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "404-102", "자재 재고 정보를 찾을 수 없습니다."),
    PRODUCTION_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "404-103", "생산계획 정보를 찾을 수 없습니다."),
    BOM_NOT_FOUND(HttpStatus.NOT_FOUND, "404-104", "BOM 정보를 찾을 수 없습니다."),
    PRODUCT_BOM_NOT_FOUND(HttpStatus.NOT_FOUND, "404-105", "해당 제품에 등록된 BOM 정보가 없습니다."),
    USER_DELETE_NOT_ALLOWED(HttpStatus.NOT_FOUND, "404-201", "삭제할 수 없는 사용자 정보입니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "404-301", "주문 정보를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "404-302", "제품 정보를 찾을 수 없습니다."),
    OPERATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "404-303", "생산 담당자 정보를 찾을 수 없습니다."),
    AVAILABLE_PRODUCTION_LINE_NOT_FOUND(HttpStatus.NOT_FOUND, "404-304", "생산 가능한 라인을 찾을 수 없습니다."),
    LINE_NOT_FOUND(HttpStatus.NOT_FOUND, "404-501", "라인 정보를 찾을 수 없습니다."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "404-401", "보고서 정보를 찾을 수 없습니다."),
    REPORT_JOB_NOT_FOUND(HttpStatus.NOT_FOUND, "404-402", "보고서 생성 작업 정보를 찾을 수 없습니다."),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "405", "지원하지 않는 HTTP 메서드입니다."),

    // 409 Conflict
    CONFLICT(HttpStatus.CONFLICT, "409", "이미 존재하는 리소스입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "409-001", "이미 사용 중인 이메일입니다."),
    DUPLICATE_MATERIAL_CODE(HttpStatus.CONFLICT, "409-101", "이미 사용 중인 자재 코드입니다."),
    DUPLICATE_BOM(HttpStatus.CONFLICT, "409-102", "이미 등록된 제품-자재 BOM 정보입니다."),
    CONCURRENT_INVENTORY_UPDATE(HttpStatus.CONFLICT, "409-103", "재고 정보가 동시에 수정되었습니다. 다시 조회 후 시도해주세요."),
    USER_ALREADY_DELETED(HttpStatus.CONFLICT, "409-201", "이미 삭제된 사용자입니다."),
    CONCURRENT_ORDER_CREATION(HttpStatus.CONFLICT, "409-301", "주문 또는 생산계획이 동시에 생성되었습니다. 다시 조회 후 시도해주세요."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "서버 내부 오류가 발생했습니다."),
    USER_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-201", "사용자 삭제 처리에 실패했습니다. 잠시 후 다시 시도해주세요."),
    REPORT_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-301", "보고서 생성에 실패했습니다."),
    AI_SERVER_CALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-302", "AI 서버 호출에 실패했습니다."),
    LINE_OPERATION_STATUS_LOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-501", "라인 가동 현황을 불러올 수 없습니다. 잠시 후 다시 조회해주세요."),
    MACHINE_OPERATION_STATUS_LOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "500-502", "설비 가동 현황을 불러올 수 없습니다. 잠시 후 다시 조회해주세요."),

    // Chat FastAPI Gateway
    CHAT_FASTAPI_CONNECTION_FAILED(HttpStatus.BAD_GATEWAY, "CHAT_FASTAPI_001", "챗봇 서버 연결에 실패했습니다."),
    CHAT_FASTAPI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "CHAT_FASTAPI_002", "챗봇 응답 시간이 초과되었습니다."),
    CHAT_FASTAPI_BAD_REQUEST(HttpStatus.BAD_GATEWAY, "CHAT_FASTAPI_003", "챗봇 요청을 처리할 수 없습니다."),
    CHAT_FASTAPI_FORBIDDEN(HttpStatus.BAD_GATEWAY, "CHAT_FASTAPI_004", "챗봇 서버 요청에 실패했습니다."),
    CHAT_FASTAPI_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CHAT_FASTAPI_005", "챗봇 서버를 사용할 수 없습니다."),
    CHAT_FASTAPI_INTERNAL_ERROR(HttpStatus.BAD_GATEWAY, "CHAT_FASTAPI_006", "챗봇 서버 내부 오류가 발생했습니다."),
    CHAT_FASTAPI_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "CHAT_FASTAPI_007", "챗봇 서버 응답을 처리할 수 없습니다."),

    // Report FastAPI Gateway
    REPORT_FASTAPI_CALL_FAILED(HttpStatus.BAD_GATEWAY, "REPORT_FASTAPI_001", "보고서 생성 서버 호출에 실패했습니다."),
    REPORT_FASTAPI_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "REPORT_FASTAPI_002", "보고서 생성 서버 응답을 처리할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
