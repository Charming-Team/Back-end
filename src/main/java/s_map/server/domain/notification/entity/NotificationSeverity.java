package s_map.server.domain.notification.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 중요도")
public enum NotificationSeverity {
    @Schema(description = "낮음")
    LOW,

    @Schema(description = "보통")
    MEDIUM,

    @Schema(description = "높음")
    HIGH
}
