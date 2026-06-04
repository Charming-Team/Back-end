package s_map.server.domain.line.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.line.repository.LineOrderDistributionLineProjection;
import s_map.server.domain.order.entity.PlanStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Schema(description = "주문별 라인 분배 상세 응답")
public record LineOrderDistributionLineResponse(

        @Schema(description = "라인 ID", example = "1")
        Long lineId,

        @Schema(description = "라인 코드", example = "LINE-ABS-01")
        String lineCode,

        @Schema(description = "라인명", example = "Line A")
        String lineName,

        @Schema(description = "제품 ID", example = "1")
        Long productId,

        @Schema(description = "제품명", example = "ABS-BLACK")
        String productName,

        @Schema(description = "제품 단위", example = "KG")
        String productUnit,

        @Schema(description = "계획 생산량", example = "4000")
        BigDecimal plannedQuantity,

        @Schema(description = "생산 수량", example = "2200")
        BigDecimal productionQuantity,

        @Schema(description = "라인별 생산 진행률. 0~1 기준", example = "0.5500")
        BigDecimal progressRate,

        @Schema(description = "라인별 생산 진행률 퍼센트", example = "55")
        Integer progressRatePercent,

        @Schema(description = "생산계획 상태", example = "IN_PROGRESS")
        PlanStatus planStatus,

        @Schema(description = "생산계획 상태 한글 표시", example = "진행 중")
        String planStatusLabel,

        @Schema(description = "전환 예정 시각", example = "2026-06-05T15:00:00+09:00")
        OffsetDateTime transitionAt,

        @Schema(description = "전환 예정 시간 표시", example = "1.2h 후")
        String transitionExpectedTime
) {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");

    public static LineOrderDistributionLineResponse from(
            LineOrderDistributionLineProjection projection,
            OffsetDateTime now
    ) {
        BigDecimal plannedQuantity = defaultZero(projection.getPlannedQuantity());
        BigDecimal productionQuantity = defaultZero(projection.getProductionQuantity());
        BigDecimal progressRate = calculateRate(productionQuantity, plannedQuantity);
        PlanStatus planStatus = toPlanStatus(projection.getPlanStatus());
        OffsetDateTime transitionAt = toKst(projection.getTransitionAt());

        return new LineOrderDistributionLineResponse(
                projection.getLineId(),
                projection.getLineCode(),
                projection.getLineName(),
                projection.getProductId(),
                projection.getProductName(),
                projection.getProductUnit(),
                plannedQuantity,
                productionQuantity,
                progressRate,
                toPercent(progressRate),
                planStatus,
                planStatus != null ? planStatus.getLabel() : "확인 필요",
                transitionAt,
                createTransitionExpectedTime(now, transitionAt)
        );
    }

    private static PlanStatus toPlanStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return PlanStatus.valueOf(value);
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

    private static OffsetDateTime toKst(Instant instant) {
        if (instant == null) {
            return null;
        }

        return instant.atZone(DEFAULT_PRODUCTION_ZONE).toOffsetDateTime();
    }

    private static String createTransitionExpectedTime(
            OffsetDateTime now,
            OffsetDateTime transitionAt
    ) {
        if (transitionAt == null) {
            return "계산 필요";
        }

        Duration duration = Duration.between(now, transitionAt);
        if (!duration.isPositive()) {
            return "전환 예정";
        }

        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + "분 후";
        }

        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);
        return hours + "h 후";
    }
}
