package s_map.server.domain.risk.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;

@Schema(description = "리스크 요약 응답")
public record RiskSummaryResponse(
        @Schema(description = "예상 지연 일수 합계", example = "12.5")
        BigDecimal expectedDelayDays,

        @Schema(description = "지연 위험 주문 수", example = "8")
        Long delayedOrderCount,

        @Schema(description = "자재 부족 발생 주문 수", example = "3")
        Long materialShortageCount,

        @Schema(description = "자재 부족 수량 합계", example = "1200")
        Long materialShortageQuantity,

        @Schema(description = "매우 위험 주문 수", example = "2")
        Long criticalOrderCount,

        @Schema(description = "전체 위험도 코드", example = "WARNING", allowableValues = {"SAFE", "CAUTION", "WARNING", "CRITICAL"})
        RiskLevel overallRiskLevel
) {
}
