package s_map.server.domain.plan.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PlanRow(
        Long planId,
        Long orderId,
        Long productId,
        String productCode,
        String productName,
        Long lineId,
        String lineCode,
        String lineName,
        Long operatorId,
        String operatorName,
        OffsetDateTime plannedStartAt,
        OffsetDateTime plannedEndAt,
        BigDecimal estimatedDurationHr,
        Integer plannedQuantity,
        Integer planSequence,
        String planStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
