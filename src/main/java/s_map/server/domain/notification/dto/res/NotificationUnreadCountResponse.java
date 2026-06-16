package s_map.server.domain.notification.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미읽음 알림 수 응답")
public record NotificationUnreadCountResponse(
        @Schema(description = "미읽음 알림 수", example = "14")
        long unreadCount
) {
}
