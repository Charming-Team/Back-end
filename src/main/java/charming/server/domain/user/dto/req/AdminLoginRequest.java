package charming.server.domain.user.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "관리자 로그인 요청")
public record AdminLoginRequest(
        @NotBlank
        @Email
        @Schema(description = "이메일", example = "admin@example.com")
        String email,

        @NotBlank
        @Schema(description = "비밀번호", example = "admin1234!")
        String password
) {
}
