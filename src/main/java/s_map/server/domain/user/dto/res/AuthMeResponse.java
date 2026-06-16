package s_map.server.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.user.entity.User;

@Schema(description = "내 정보 조회 응답")
@Getter
@Builder
public class AuthMeResponse {

    @Schema(description = "사용자 ID", example = "16")
    private Long id;

    @Schema(description = "사용자 이름", example = "나관리")
    private String name;

    @Schema(description = "이메일", example = "manager@sk.com")
    private String email;

    @Schema(description = "사용자 권한", example = "MANUFACTURING_MANAGER")
    private String role;

    @Schema(description = "부서명", example = "생산관리팀")
    private String department;

    @Schema(description = "회사명", example = "SK")
    private String companyName;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phoneNumber;

    public static AuthMeResponse from(User user) {
        return AuthMeResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .department(user.getDepartment())
                .companyName(user.getCompanyName())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}
