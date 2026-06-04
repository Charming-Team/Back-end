package s_map.server.domain.line.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.line.repository.LineOrderDistributionLineProjection;
import s_map.server.domain.line.repository.LineOrderDistributionSummaryProjection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Schema(description = "주문별 생산 라인 분배 현황 응답")
public record LineOrderDistributionResponse(

        @Schema(description = "주문 ID", example = "1")
        Long orderId,

        @Schema(description = "주문 번호", example = "PO-240520-001")
        String orderNo,

        @Schema(description = "제품 ID", example = "1")
        Long productId,

        @Schema(description = "제품명", example = "ABS-BLACK")
        String productName,

        @Schema(description = "제품 단위", example = "KG")
        String productUnit,

        @Schema(description = "주문 수량", example = "10000")
        Integer orderQuantity,

        @Schema(description = "납기일", example = "2026-06-20")
        LocalDate dueDate,

        @Schema(description = "배정된 생산 라인 수", example = "3")
        Integer assignedLineCount,

        @Schema(description = "총 계획 생산 수량", example = "10000")
        BigDecimal totalPlannedQuantity,

        @Schema(description = "총 생산 수량", example = "5500")
        BigDecimal totalProductionQuantity,

        @Schema(description = "전체 생산 진행률. 0~1 기준", example = "0.5500")
        BigDecimal progressRate,

        @Schema(description = "전체 생산 진행률 퍼센트", example = "55")
        Integer progressRatePercent,

        @Schema(description = "납기일까지 남은 일수. 납기 초과 시 음수", example = "20")
        Long daysUntilDueDate,

        @Schema(description = "납기일까지 남은 기간 표시", example = "20일")
        String daysUntilDueDateLabel,

        @Schema(description = "라인별 분배 현황")
        List<LineOrderDistributionLineResponse> lines
) {

    public static LineOrderDistributionResponse of(
            LineOrderDistributionSummaryProjection summary,
            List<LineOrderDistributionLineProjection> lineProjections,
            LocalDate today,
            OffsetDateTime now
    ) {
        BigDecimal totalProductionQuantity = defaultZero(summary.getTotalProductionQuantity());
        BigDecimal progressRate = calculateRate(
                totalProductionQuantity,
                toBigDecimal(summary.getOrderQuantity())
        );
        Long daysUntilDueDate = ChronoUnit.DAYS.between(today, summary.getDueDate());

        return new LineOrderDistributionResponse(
                summary.getOrderId(),
                summary.getOrderNo(),
                summary.getProductId(),
                summary.getProductName(),
                summary.getProductUnit(),
                summary.getOrderQuantity(),
                summary.getDueDate(),
                summary.getAssignedLineCount(),
                defaultZero(summary.getTotalPlannedQuantity()),
                totalProductionQuantity,
                progressRate,
                toPercent(progressRate),
                daysUntilDueDate,
                createDaysUntilDueDateLabel(daysUntilDueDate),
                lineProjections.stream()
                        .map(line -> LineOrderDistributionLineResponse.from(line, now))
                        .toList()
        );
    }

    private static BigDecimal toBigDecimal(Integer value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(value);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private static BigDecimal calculateRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return defaultZero(numerator).divide(denominator, 4, RoundingMode.HALF_UP);
    }

    private static Integer toPercent(BigDecimal rate) {
        return defaultZero(rate)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private static String createDaysUntilDueDateLabel(Long daysUntilDueDate) {
        if (daysUntilDueDate == null) {
            return "확인 필요";
        }

        if (daysUntilDueDate == 0) {
            return "오늘";
        }

        if (daysUntilDueDate > 0) {
            return daysUntilDueDate + "일";
        }

        return Math.abs(daysUntilDueDate) + "일 지남";
    }
}
