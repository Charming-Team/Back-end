package charming.server.domain.user.dto.res;

import charming.server.domain.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 로그인 응답")
public record AdminLoginResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "사용자 이름", example = "관리자")
        String name,

        @Schema(description = "이메일", example = "admin@example.com")
        String email,

        @Schema(description = "사용자 권한", example = "ADMIN")
        Role role
) {
}
