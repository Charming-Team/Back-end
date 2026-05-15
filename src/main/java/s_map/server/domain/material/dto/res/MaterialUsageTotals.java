package s_map.server.domain.material.dto.res;

import java.math.BigDecimal;

public record MaterialUsageTotals(
        BigDecimal totalExpectedUsage,
        BigDecimal totalReservedQuantity,
        BigDecimal totalConsumedQuantity,
        BigDecimal totalShortageQuantity
) {

    public MaterialUsageTotals {
        totalExpectedUsage = zeroIfNull(totalExpectedUsage);
        totalReservedQuantity = zeroIfNull(totalReservedQuantity);
        totalConsumedQuantity = zeroIfNull(totalConsumedQuantity);
        totalShortageQuantity = zeroIfNull(totalShortageQuantity);
    }

    private static BigDecimal zeroIfNull(BigDecimal quantity) {
        return quantity != null ? quantity : BigDecimal.ZERO;
    }
}
