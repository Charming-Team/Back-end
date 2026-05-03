package charming.server.domain.user.dto.res;

import charming.server.domain.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "사용자 이름", example = "김길동")
        String name,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "사용자 권한", example = "OPERATOR")
        Role role,

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
