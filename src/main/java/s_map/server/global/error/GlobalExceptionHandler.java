package s_map.server.global.error;

import s_map.server.global.common.BaseResponse;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<BaseResponse<Void>> handleCustomException(CustomException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.fail(errorCode.getCode(), exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse(errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.fail(errorCode.getCode(), message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .orElse(errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.fail(errorCode.getCode(), message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpMessageNotReadableException() {
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.fail(errorCode));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Void>> handleMethodArgumentTypeMismatchException() {
        ErrorCode errorCode = ErrorCode.BAD_REQUEST;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.fail(errorCode));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<Void>> handleHttpRequestMethodNotSupportedException() {
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.fail(errorCode));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<BaseResponse<Void>> handleAuthenticationException() {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.fail(errorCode));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDeniedException() {
        ErrorCode errorCode = ErrorCode.FORBIDDEN;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.fail(errorCode));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<BaseResponse<Void>> handleOptimisticLockingFailureException() {
        ErrorCode errorCode = ErrorCode.CONCURRENT_INVENTORY_UPDATE;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.fail(errorCode));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        log.warn(
                "[GlobalExceptionHandler] DB 무결성 제약 위반 reason=data_integrity_violation path={} rootCause={}",
                request.getRequestURI(),
                rootCauseMessage(exception)
        );
        ErrorCode errorCode = ErrorCode.CONFLICT;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.fail(errorCode));
    }

    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsableException(
            AsyncRequestNotUsableException exception,
            HttpServletRequest request
    ) {
        log.debug(
                "[GlobalExceptionHandler] 비동기 요청 클라이언트 연결 종료 path={} message={}",
                request.getRequestURI(),
                exception.getMessage()
        );
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    public void handleAsyncRequestTimeoutException(
            AsyncRequestTimeoutException exception,
            HttpServletRequest request
    ) {
        log.debug(
                "[GlobalExceptionHandler] 비동기 요청 타임아웃으로 연결 종료 path={} message={}",
                request.getRequestURI(),
                exception.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleException(Exception exception, HttpServletRequest request) {
        log.error(
                "[GlobalExceptionHandler] 처리되지 않은 예외 발생 reason=unhandled_exception path={}",
                request.getRequestURI(),
                exception
        );
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(BaseResponse.fail(errorCode));
    }

    private String rootCauseMessage(Throwable exception) {
        Throwable rootCause = exception;
        while (rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }

        return rootCause.getMessage();
    }
}
