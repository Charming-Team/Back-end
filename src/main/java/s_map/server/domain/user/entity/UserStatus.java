package s_map.server.domain.user.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 계정 상태")
public enum UserStatus {
    @Schema(description = "정상")
    ACTIVE,

    @Schema(description = "일시 정지")
    SUSPENDED,

    @Schema(description = "영구 정지")
    BANNED,

    @Schema(description = "탈퇴")
    WITHDRAWN
}
