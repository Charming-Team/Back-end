package s_map.server.global.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

@Component
@RequiredArgsConstructor
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
        if (evidenceToken == null
                || evidenceToken.isBlank()
                || internalToken == null
                || !evidenceToken.equals(internalToken)) {
            throw new CustomException(ErrorCode.INVALID_INTERNAL_TOKEN);
        }
    }
}
