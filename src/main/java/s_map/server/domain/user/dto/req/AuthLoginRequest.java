package s_map.server.domain.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "로그인 요청")
public record AuthLoginRequest(
        @NotBlank
        @Email
        @Pattern(
                regexp = "(?i)^[a-z0-9._%+-]+@sk\\.com$",
                message = "이메일은 sk.com 도메인만 사용할 수 있습니다."
        )
        @Schema(description = "이메일", example = "user@sk.com")
        String email,

        @NotBlank
        @Schema(description = "비밀번호", example = "Password1234!")
        String password
) {
}
