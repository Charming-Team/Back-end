package s_map.server.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.user.entity.User;

@Getter
@Builder
@Schema(description = "관리자 사용자 조회 응답")
public class AdminUserResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "사용자 이름", example = "김길동")
    private String name;

    @Schema(description = "이메일", example = "operator01@example.com")
    private String email;

    @Schema(description = "권한", example = "OPERATOR")
    private String role;

    @Schema(description = "계정 상태", example = "ACTIVE")
    private String status;

    @Schema(description = "소속 부서", example = "생산관리팀")
    private String department;

    @Schema(description = "회사명", example = "s_map")
    private String companyName;

    @Schema(description = "연락처", example = "010-1234-5678")
    private String phoneNumber;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .department(user.getDepartment())
                .companyName(user.getCompanyName())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}