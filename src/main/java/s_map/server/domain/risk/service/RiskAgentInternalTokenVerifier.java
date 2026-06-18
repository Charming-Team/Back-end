package s_map.server.domain.risk.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class RiskAgentInternalTokenVerifier {

    private final String expectedToken;

    public RiskAgentInternalTokenVerifier(
            @Value("${ai.fastapi.risk.internal-token:}") String expectedToken
    ) {
        this.expectedToken = expectedToken;
    }

    public void verify(String actualToken) {
        if (expectedToken == null || expectedToken.isBlank()) {
            throw new CustomException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "Risk Agent internal token이 설정되지 않았습니다."
            );
        }

        if (actualToken == null || actualToken.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INTERNAL_TOKEN);
        }

        boolean matched = MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                actualToken.getBytes(StandardCharsets.UTF_8)
        );

        if (!matched) {
            throw new CustomException(ErrorCode.INVALID_INTERNAL_TOKEN);
        }
    }
}