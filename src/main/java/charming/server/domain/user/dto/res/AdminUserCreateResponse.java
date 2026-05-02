package charming.server.domain.user.dto.res;

import charming.server.domain.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 사용자 생성 응답")
public record AdminUserCreateResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long id,

        @Schema(description = "사용자 이름", example = "김길동")
        String name,

        @Schema(description = "이메일", example = "operator01@example.com")
        String email,

        @Schema(description = "사용자 권한", example = "OPERATOR")
        Role role,

        @Schema(description = "소속 부서", example = "생산관리팀")
        String department,

        @Schema(description = "연락처", example = "010-1234-5678")
        String phoneNumber
) {
}
