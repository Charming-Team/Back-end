package s_map.server.domain.user.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 권한")
public enum Role {
    @Schema(description = "시스템관리자")
    ADMIN,

    @Schema(description = "작업자")
    OPERATOR,

    @Schema(description = "경영진")
    EXECUTIVE,

    @Schema(description = "생산관리자")
    MANUFACTURING_MANAGER
}
