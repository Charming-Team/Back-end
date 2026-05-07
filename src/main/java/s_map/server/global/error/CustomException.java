package s_map.server.global.error;

import s_map.server.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final s_map.server.global.error.ErrorCode errorCode;

    public CustomException(s_map.server.global.error.ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
