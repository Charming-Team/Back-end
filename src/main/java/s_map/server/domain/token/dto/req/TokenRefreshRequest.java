package s_map.server.domain.token.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 재발급 요청")
public record TokenRefreshRequest(
        @NotBlank
        @Schema(description = "Refresh Token")
        String refreshToken
) {
}
