package s_map.server.global.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalApiTokenValidator {

    @Value("${app.internal.chat.evidence-token:}")
    private String evidenceToken;

    /**
     * 기능: FastAPI 챗봇 서버가 Spring Evidence 내부 API를 호출할 권한이 있는지 검증한다.
     *
     * Input:
     * - internalToken / String / X-Internal-Token 헤더 값
     *
     * Output:
     * - result / void / 반환값 없음, 토큰 누락 또는 불일치 시 INVALID_INTERNAL_TOKEN 예외 발생
     */
    public void validateEvidenceToken(String internalToken) {
        if (evidenceToken == null || evidenceToken.isBlank()) {
            log.error("[InternalApiTokenValidator] 내부 Evidence 토큰 검증 실패 reason=config_missing");
            throw new CustomException(ErrorCode.INVALID_INTERNAL_TOKEN);
        }

        if (internalToken == null) {
            log.warn("[InternalApiTokenValidator] 내부 Evidence 토큰 검증 실패 reason=missing_header");
            throw new CustomException(ErrorCode.INVALID_INTERNAL_TOKEN);
        }

        if (!evidenceToken.equals(internalToken)) {
            log.warn("[InternalApiTokenValidator] 내부 Evidence 토큰 검증 실패 reason=token_mismatch");
            throw new CustomException(ErrorCode.INVALID_INTERNAL_TOKEN);
        }
    }
}
