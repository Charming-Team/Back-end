package s_map.server.domain.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "로그아웃 요청")
@Getter
@NoArgsConstructor
public class LogoutRequest {

    @NotBlank(message = "Refresh Token은 필수입니다.")
    @Schema(description = "폐기할 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
