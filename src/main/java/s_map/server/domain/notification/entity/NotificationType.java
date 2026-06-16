package s_map.server.domain.notification.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 유형")
public enum NotificationType {
    @Schema(description = "납기 지연")
    DELAY_RISK,

    @Schema(description = "AI 예측 결과 준비 완료")
    PREDICTION_READY,

    @Schema(description = "보고서 생성 완료")
    REPORT_READY,

    @Schema(description = "생산 계획 변경")
    SCHEDULE_APPLIED,

    @Schema(description = "시스템 오류")
    SYSTEM_ERROR
}
