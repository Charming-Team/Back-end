package s_map.server.domain.user.dto.req;

import s_map.server.domain.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 사용자 생성 요청")
public record AdminUserCreateRequest(
        @NotBlank
        @Size(max = 50)
        @Schema(description = "사용자 이름", example = "김길동")
        String name,

        @NotBlank
        @Email
        @Size(max = 100)
        @Schema(description = "이메일", example = "operator01@example.com")
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        @Schema(description = "비밀번호", example = "operator1234!")
        String password,

        @NotBlank
        @Size(min = 8, max = 100)
        @Schema(description = "비밀번호 확인", example = "operator1234!")
        String passwordConfirm,

        @NotNull
        @Schema(description = "사용자 권한", example = "OPERATOR")
        Role role,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "소속 부서", example = "생산관리팀")
        String department,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "회사명", example = "s_map")
        String companyName,

        @NotBlank
        @Size(max = 20)
        @Schema(description = "연락처", example = "010-1234-5678")
        String phoneNumber
) {
}
