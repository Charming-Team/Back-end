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

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "401", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "401-001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "401-002", "만료된 토큰입니다."),
    INVALID_LOGIN_CREDENTIALS(HttpStatus.UNAUTHORIZED, "401-003", "아이디 또는 비밀번호가 올바르지 않습니다."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "403", "접근 권한이 없습니다."),
    ADMIN_AUTH_REQUIRED(HttpStatus.FORBIDDEN, "403-001", "관리자만 로그인할 수 있습니다."),
    INACTIVE_ACCOUNT(HttpStatus.FORBIDDEN, "403-002", "사용할 수 없는 계정입니다."),

    // 404 Not Found
    NOT_FOUND(HttpStatus.NOT_FOUND, "404", "요청한 리소스를 찾을 수 없습니다."),
    MATERIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "404-101", "자재 정보를 찾을 수 없습니다."),
    MATERIAL_INVENTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "404-102", "자재 재고 정보를 찾을 수 없습니다."),
    PRODUCTION_PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "404-103", "생산계획 정보를 찾을 수 없습니다."),
    BOM_NOT_FOUND(HttpStatus.NOT_FOUND, "404-104", "BOM 정보를 찾을 수 없습니다."),
    PRODUCT_BOM_NOT_FOUND(HttpStatus.NOT_FOUND, "404-105", "해당 제품에 등록된 BOM 정보가 없습니다."),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "405", "지원하지 않는 HTTP 메서드입니다."),

    // 409 Conflict
    CONFLICT(HttpStatus.CONFLICT, "409", "이미 존재하는 리소스입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "409-001", "이미 사용 중인 이메일입니다."),
    DUPLICATE_MATERIAL_CODE(HttpStatus.CONFLICT, "409-101", "이미 사용 중인 자재 코드입니다."),
    DUPLICATE_BOM(HttpStatus.CONFLICT, "409-102", "이미 등록된 제품-자재 BOM 정보입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "500", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
