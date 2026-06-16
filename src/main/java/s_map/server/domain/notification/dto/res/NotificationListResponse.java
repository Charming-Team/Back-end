package s_map.server.domain.notification.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "알림 목록 무한스크롤 응답")
public record NotificationListResponse(
        @Schema(description = "알림 목록")
        List<NotificationResponse> items,

        @Schema(description = "다음 목록 조회 cursor. 다음 페이지가 없으면 null", example = "991", nullable = true)
        Long nextCursor,

        @Schema(description = "다음 목록 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "미읽음 알림 수", example = "14")
        long unreadCount
) {
}
