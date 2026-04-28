package charming.server.global.common;

import charming.server.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class BaseResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    // 성공 응답 (data 포함)
    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.<T>builder()
                .success(true)
                .code("COMMON200")
                .message("요청 성공")
                .data(data)
                .build();
    }

    // 성공 응답 (data 없음)
    public static BaseResponse<Void> success() {
        return BaseResponse.<Void>builder()
                .success(true)
                .code("COMMON200")
                .message("요청 성공")
                .data(null)
                .build();
    }

    // 실패 응답
    public static BaseResponse<Void> fail(ErrorCode errorCode) {
        return BaseResponse.<Void>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .data(null)
                .build();
    }

    public static BaseResponse<Void> fail(String code, String message) {
        return BaseResponse.<Void>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(null)
                .build();
    }

    public static <T> BaseResponse<T> fail(String code, String message, T data) {
        return BaseResponse.<T>builder()
                .success(false)
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
}
