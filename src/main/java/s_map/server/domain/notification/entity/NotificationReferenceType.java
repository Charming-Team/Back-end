package s_map.server.domain.notification.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 참조 대상 유형")
public enum NotificationReferenceType {
    @Schema(description = "주문")
    ORDER,

    @Schema(description = "예측 결과")
    PREDICTION,

    @Schema(description = "자재")
    MATERIAL,

    @Schema(description = "보고서")
    REPORT,

    @Schema(description = "생산계획")
    PLAN,

    @Schema(description = "라인")
    LINE,

    @Schema(description = "설비")
    MACHINE,

    @Schema(description = "시스템")
    SYSTEM
}
