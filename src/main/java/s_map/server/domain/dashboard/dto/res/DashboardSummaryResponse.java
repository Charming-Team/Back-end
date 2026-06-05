package s_map.server.domain.dashboard.dto.res;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DashboardSummaryResponse(
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        long delayRiskOrderCount,
        BigDecimal delayRiskOrderRate,
        long materialShortageCount,
        BigDecimal materialShortageRate,
        BigDecimal orderAchievementRate,
        BigDecimal savedDelayDays,
        String savedDelayUnit,
        OffsetDateTime lastUpdatedAt
) {

    public static DashboardSummaryResponse of(
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            long totalOrderCount,
            long delayRiskOrderCount,
            long totalMaterialTargetCount,
            long materialShortageCount,
            long dueTargetOrderCount,
            long onTimeCompletedOrderCount,
            BigDecimal delayReductionHours,
            OffsetDateTime lastUpdatedAt
    ) {
        return new DashboardSummaryResponse(
                periodStartDate,
                periodEndDate,
                delayRiskOrderCount,
                calculateRate(delayRiskOrderCount, totalOrderCount),
                materialShortageCount,
                calculateRate(materialShortageCount, totalMaterialTargetCount),
                calculateRate(onTimeCompletedOrderCount, dueTargetOrderCount),
                toDays(delayReductionHours),
                "DAY",
                lastUpdatedAt
        );
    }

    private static BigDecimal calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private static BigDecimal toDays(BigDecimal hours) {
        if (hours == null) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return hours.divide(BigDecimal.valueOf(24), 1, RoundingMode.HALF_UP);
    }
}