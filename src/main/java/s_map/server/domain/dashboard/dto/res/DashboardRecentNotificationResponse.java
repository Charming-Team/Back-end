package s_map.server.domain.dashboard.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.dashboard.repository.DashboardRepository.RecentNotificationRow;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "대시보드 최근 알림 응답")
public record DashboardRecentNotificationResponse(
        @Schema(description = "미읽음 알림 수", example = "10")
        long unreadCount,

        @Schema(description = "최근 알림 목록")
        List<RecentNotificationItem> notifications
) {

    public static DashboardRecentNotificationResponse of(
            long unreadCount,
            List<RecentNotificationRow> rows
    ) {
        return new DashboardRecentNotificationResponse(
                unreadCount,
                rows.stream()
                        .map(RecentNotificationItem::from)
                        .toList()
        );
    }

    public record RecentNotificationItem(
            @Schema(description = "알림 ID", example = "1")
            Long notificationId,

            @Schema(description = "알림 유형", example = "DELAY_RISK")
            String notificationType,

            @Schema(description = "알림 제목", example = "지연 위험 주문 발생")
            String title,

            @Schema(description = "알림 내용", example = "PO-240520-001 주문의 지연 위험이 감지되었습니다.")
            String content,

            @Schema(description = "알림 심각도", example = "WARNING")
            String severity,

            @Schema(description = "읽음 여부", example = "false")
            Boolean isRead,

            @Schema(description = "참조 대상 유형", example = "ORDER")
            String referenceType,

            @Schema(description = "참조 대상 ID", example = "2001", nullable = true)
            Long referenceId,

            @Schema(description = "알림 생성 시각", example = "2026-06-06T10:30:00+09:00")
            OffsetDateTime createdAt
    ) {

        public static RecentNotificationItem from(RecentNotificationRow row) {
            return new RecentNotificationItem(
                    row.notificationId(),
                    row.notificationType(),
                    row.title(),
                    row.content(),
                    row.severity(),
                    row.isRead(),
                    row.referenceType(),
                    row.referenceId(),
                    row.createdAt()
            );
        }
    }
}
