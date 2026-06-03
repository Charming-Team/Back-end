package s_map.server.domain.user.dto.req;

import s_map.server.domain.user.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 사용자 생성 요청")
public record AdminUserCreateRequest(
        @NotBlank
        @Size(max = 50)
        @Schema(description = "사용자 이름", example = "김길동")
        String name,

        @NotBlank
        @Email
        @Pattern(
                regexp = "(?i)^[a-z0-9._%+-]+@sk\\.com$",
                message = "이메일은 sk.com 도메인만 사용할 수 있습니다."
        )
        @Size(max = 100)
        @Schema(description = "이메일", example = "operator01@sk.com")
        String email,

        @NotBlank
        @Size(min = 11, max = 100)
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[!@#$~])(?=(?:.*\\d){2,}).{11,100}$",
                message = "비밀번호는 대문자 1개, 특수기호(!,@,#,$,~) 1개, 숫자 2개를 포함하고 11자 이상이어야 합니다."
        )
        @Schema(description = "비밀번호", example = "Operator1234!")
        String password,

        @NotBlank
        @Size(min = 11, max = 100)
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[!@#$~])(?=(?:.*\\d){2,}).{11,100}$",
                message = "비밀번호 확인은 대문자 1개, 특수기호(!,@,#,$,~) 1개, 숫자 2개를 포함하고 11자 이상이어야 합니다."
        )
        @Schema(description = "비밀번호 확인", example = "Operator1234!")
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
        @Pattern(
                regexp = "^010-\\d{4}-\\d{4}$",
                message = "연락처는 010-1234-5678 형식이어야 합니다."
        )
        @Schema(description = "연락처", example = "010-1234-5678")
        String phoneNumber
) {
}
