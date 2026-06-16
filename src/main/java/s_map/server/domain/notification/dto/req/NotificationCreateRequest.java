package s_map.server.domain.notification.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import s_map.server.domain.notification.entity.NotificationReferenceType;
import s_map.server.domain.notification.entity.NotificationSeverity;
import s_map.server.domain.notification.entity.NotificationType;

@Schema(description = "알림 생성 요청")
public record NotificationCreateRequest(
        @Schema(description = "수신 사용자 ID", example = "16")
        @NotNull
        Long recipientUserId,

        @Schema(description = "알림 유형", example = "REPORT_READY")
        @NotNull
        NotificationType notificationType,

        @Schema(description = "알림 제목", example = "보고서 생성 완료")
        @NotBlank
        String title,

        @Schema(description = "알림 내용", example = "요청한 보고서 생성이 완료되었습니다.")
        @NotBlank
        String content,

        @Schema(description = "알림 클릭 시 이동할 URL", example = "/reports/12", nullable = true)
        String url,

        @Schema(description = "중요도", example = "LOW")
        @NotNull
        NotificationSeverity severity,

        @Schema(description = "참조 대상 유형", example = "REPORT", nullable = true)
        NotificationReferenceType referenceType,

        @Schema(description = "참조 대상 ID", example = "12", nullable = true)
        Long referenceId
) {
}
