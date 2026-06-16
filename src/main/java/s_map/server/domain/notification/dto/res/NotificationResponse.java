package s_map.server.domain.notification.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.notification.entity.NotificationReferenceType;
import s_map.server.domain.notification.entity.NotificationSeverity;
import s_map.server.domain.notification.entity.NotificationType;

import java.time.OffsetDateTime;

@Schema(description = "알림 응답")
public record NotificationResponse(
        @Schema(description = "알림 ID", example = "1001")
        Long notificationId,

        @Schema(description = "알림 유형", example = "DELAY_RISK")
        NotificationType notificationType,

        @Schema(description = "알림 제목", example = "납기 지연 위험 발생")
        String title,

        @Schema(description = "알림 내용", example = "PO-260601-001 주문의 납기 지연 위험이 감지되었습니다.")
        String content,

        @Schema(description = "생성 시각", example = "2026-06-16T10:30:00+09:00")
        OffsetDateTime createdAt,

        @Schema(description = "읽음 여부", example = "false")
        Boolean isRead,

        @Schema(description = "알림 클릭 시 이동할 URL", example = "/risk?orderId=431")
        String url,

        @Schema(description = "중요도", example = "MEDIUM")
        NotificationSeverity severity,

        @Schema(description = "참조 대상 유형", example = "ORDER", nullable = true)
        NotificationReferenceType referenceType,

        @Schema(description = "참조 대상 ID", example = "431", nullable = true)
        Long referenceId
) {
}
