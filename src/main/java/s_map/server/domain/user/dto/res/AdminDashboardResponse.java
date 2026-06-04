package s_map.server.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import s_map.server.domain.user.entity.Role;

@Schema(description = "관리자 대시보드 응답")
public record AdminDashboardResponse(
        @Schema(description = "전체 사용자 수. 탈퇴 사용자는 제외합니다.", example = "124")
        long totalUsers,

        @Schema(description = "활성 사용자 수", example = "98")
        long activeUsers,

        @Schema(description = "권한별 사용자 분포")
        List<RoleDistributionResponse> roleDistribution
) {

    @Schema(description = "권한별 사용자 수")
    public record RoleDistributionResponse(
            @Schema(description = "사용자 권한", example = "ADMIN")
            Role role,

            @Schema(description = "관리자 화면 표시명", example = "서버관리자")
            String roleName,

            @Schema(description = "해당 권한 사용자 수", example = "3")
            long count
    ) {
    }
}
