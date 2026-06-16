package s_map.server.domain.risk.dto.res;

import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;

public record RiskSummaryResponse(
        BigDecimal expectedDelayDays,
        Long delayedOrderCount,
        Long materialShortageCount,
        Long materialShortageQuantity,
        Long criticalOrderCount,
        RiskLevel overallRiskLevel
) {
}