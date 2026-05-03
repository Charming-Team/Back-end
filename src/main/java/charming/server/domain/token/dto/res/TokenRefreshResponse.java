package charming.server.domain.token.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "토큰 재발급 응답")
public record TokenRefreshResponse(
        @Schema(description = "토큰 타입", example = "Bearer")
        String tokenType,

        @Schema(description = "Access Token")
        String accessToken,

        @Schema(description = "Refresh Token")
        String refreshToken,

        @Schema(description = "Access Token 만료 시간(ms)", example = "300000")
        long accessTokenExpiresIn,

        @Schema(description = "Refresh Token 만료 시간(ms)", example = "86400000")
        long refreshTokenExpiresIn
) {
}
