package s_map.server.domain.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청")
public record LoginRequest(
        @NotBlank
        @Email
        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @NotBlank
        @Schema(description = "비밀번호", example = "password1234!")
        String password
) {
}
