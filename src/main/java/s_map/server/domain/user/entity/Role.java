package s_map.server.domain.user.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 권한")
public enum Role {
    @Schema(description = "서버관리자")
    ADMIN,

    @Schema(description = "작업자")
    OPERATOR,

    @Schema(description = "경영진")
    EXECUTIVE,

    @Schema(description = "제조관리직")
    MANUFACTURING_MANAGER
}
