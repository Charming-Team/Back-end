package s_map.server.domain.risk.dto.res;

import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record RiskOrderListItemResponse(
        Long id,
        Long orderId,
        String orderNo,
        String customerName,
        String productName,
        String productGroup,
        Integer quantity,
        Integer completedQuantity,
        Integer remainingQuantity,
        LocalDate dueDate,
        BigDecimal progressRate,
        Integer progressRatePercent,
        String lineName,
        RiskLevel riskLevel,
        BigDecimal delayProbability,
        BigDecimal delayProbabilityPercent,
        OffsetDateTime predictedAt
) {
}
