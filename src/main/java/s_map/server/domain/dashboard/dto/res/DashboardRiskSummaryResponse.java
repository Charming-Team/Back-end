package s_map.server.domain.dashboard.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.dashboard.repository.DashboardRepository.RecentRiskRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "대시보드 리스크 요약 응답")
public record DashboardRiskSummaryResponse(
        @Schema(description = "리스크 집계 시작일", example = "2026-06-01")
        LocalDate periodStartDate,

        @Schema(description = "리스크 집계 종료일", example = "2026-06-06")
        LocalDate periodEndDate,

        @Schema(description = "지연 위험 주문 수", example = "18")
        long delayRiskOrderCount,

        @Schema(description = "생산계획 기준 자재 리스크 품목 수", example = "2")
        long materialRiskCount,

        @Schema(description = "활성 라인 중 최신 상태가 위험 상태인 라인 수", example = "1")
        long lineRiskCount,

        @Schema(description = "CRITICAL 위험 주문 수", example = "5")
        long criticalRiskCount,

        @Schema(description = "WARNING 위험 주문 수", example = "13")
        long warningRiskCount,

        @Schema(description = "최근 위험 주문 목록")
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
            @Schema(description = "AI 예측 결과 ID", example = "1001")
            Long predictionId,

            @Schema(description = "주문 ID", example = "2001")
            Long orderId,

            @Schema(description = "주문 번호", example = "PO-240520-001")
            String orderNo,

            @Schema(description = "제품명", example = "ABS-Black")
            String productName,

            @Schema(description = "위험 등급", example = "WARNING", allowableValues = {"WARNING", "CRITICAL"})
            String riskLevel,

            @Schema(description = "지연 확률", example = "0.82")
            BigDecimal delayProbability,

            @Schema(description = "예상 지연 일수", example = "2.5")
            BigDecimal predictedDelayDays,

            @Schema(description = "주요 위험 원인 목록. 현재는 빈 배열로 내려갈 수 있습니다.", example = "[\"자재 부족\", \"라인 정지\"]")
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
