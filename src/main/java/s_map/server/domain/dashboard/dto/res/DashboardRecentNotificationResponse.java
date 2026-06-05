package s_map.server.domain.dashboard.dto.res;

import s_map.server.domain.dashboard.repository.DashboardRepository.RecentNotificationRow;

import java.time.OffsetDateTime;
import java.util.List;

public record DashboardRecentNotificationResponse(
        long unreadCount,
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
            Long notificationId,
            String notificationType,
            String title,
            String content,
            String severity,
            Boolean isRead,
            String referenceType,
            Long referenceId,
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