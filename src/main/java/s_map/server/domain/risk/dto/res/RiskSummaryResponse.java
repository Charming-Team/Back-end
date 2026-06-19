package s_map.server.domain.risk.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;

@Schema(description = "리스크 요약 응답")
public record RiskSummaryResponse(
        @Schema(description = "지연 위험 주문의 예상 지연 일수 합계. SAFE 주문은 제외합니다.", example = "12.5")
        BigDecimal expectedDelayDays,

        @Schema(description = "최신 AI 예측 결과 기준 지연 위험 주문 수. SAFE 주문은 제외합니다.", example = "18")
        Long delayedOrderCount,

        @Schema(description = "현재 재고 기준 SHORTAGE 상태 자재 품목 수", example = "4")
        Long materialShortageCount,

        @Schema(description = "현재 재고의 안전 재고 대비 부족 수량 합계", example = "320")
        Long materialShortageQuantity,

        @Schema(description = "CRITICAL 위험 주문 수", example = "5")
        Long criticalOrderCount,

        @Schema(description = "전체 위험 등급", example = "WARNING", allowableValues = {"SAFE", "CAUTION", "WARNING", "CRITICAL"})
        RiskLevel overallRiskLevel
) {
}
