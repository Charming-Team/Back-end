package s_map.server.domain.risk.dto.res;

import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record RiskOrderDetailResponse(
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
        String lineName,

        RiskLevel riskLevel,
        String riskLevelLabel,
        BigDecimal delayProbability,
        BigDecimal delayProbabilityPercent,
        OffsetDateTime predictedAt,

        BigDecimal expectedDelayDays,
        String title,
        List<String> causeTypes,
        String summary,
        String progressMessage,
        String recommendation,
        List<RiskCauseResponse> causes,
        Boolean hasAgentAnalysis
) {
}