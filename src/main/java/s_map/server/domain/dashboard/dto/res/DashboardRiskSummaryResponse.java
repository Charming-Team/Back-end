package s_map.server.domain.dashboard.dto.res;

import s_map.server.domain.dashboard.repository.DashboardRepository.RecentRiskRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardRiskSummaryResponse(
        LocalDate periodStartDate,
        LocalDate periodEndDate,
        long delayRiskOrderCount,
        long materialRiskCount,
        long lineRiskCount,
        long criticalRiskCount,
        long warningRiskCount,
        List<RecentRiskItem> recentRisks
) {

    public static DashboardRiskSummaryResponse of(
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            long delayRiskOrderCount,
            long materialRiskCount,
            long lineRiskCount,
            long criticalRiskCount,
            long warningRiskCount,
            List<RecentRiskRow> rows
    ) {
        return new DashboardRiskSummaryResponse(
                periodStartDate,
                periodEndDate,
                delayRiskOrderCount,
                materialRiskCount,
                lineRiskCount,
                criticalRiskCount,
                warningRiskCount,
                rows.stream()
                        .map(RecentRiskItem::from)
                        .toList()
        );
    }

    public record RecentRiskItem(
            Long predictionId,
            Long orderId,
            String orderNo,
            String productName,
            String riskLevel,
            BigDecimal delayProbability,
            BigDecimal predictedDelayDays,
            List<String> causes
    ) {

        public static RecentRiskItem from(RecentRiskRow row) {
            return new RecentRiskItem(
                    row.predictionId(),
                    row.orderId(),
                    row.orderNo(),
                    row.productName(),
                    row.riskLevel(),
                    row.delayProbability(),
                    row.predictedDelayDays(),
                    row.causes()
            );
        }
    }
}