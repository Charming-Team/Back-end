package s_map.server.domain.plan.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ProductionResultRow(
        Long planId,
        OffsetDateTime actualStartAt,
        OffsetDateTime actualEndAt,
        BigDecimal actualQuantity,
        BigDecimal defectQuantity,
        BigDecimal yieldRate
) {
}
