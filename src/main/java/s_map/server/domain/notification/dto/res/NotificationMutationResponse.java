package s_map.server.domain.notification.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 변경 처리 응답")
public record NotificationMutationResponse(
        @Schema(description = "처리된 알림 수", example = "3")
        int affectedCount,

        @Schema(description = "처리 후 미읽음 알림 수", example = "11")
        long unreadCount
) {
}
