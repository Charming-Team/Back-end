package s_map.server.domain.dashboard.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "대시보드 상단 KPI 요약 응답")
public record DashboardSummaryResponse(
        @Schema(description = "집계 시작일", example = "2026-06-01")
        LocalDate periodStartDate,

        @Schema(description = "집계 종료일", example = "2026-06-06")
        LocalDate periodEndDate,

        @Schema(description = "최신 AI 예측 결과 기준 지연 위험 주문 수. 완료/취소 주문과 예측 대상이 아닌 주문은 제외합니다.", example = "18")
        long delayRiskOrderCount,

        @Schema(description = "월간 생산 대상 주문 대비 지연 위험 주문 비율", example = "8.0")
        BigDecimal delayRiskOrderRate,

        @Schema(description = "현재 재고 기준 SHORTAGE 상태 자재 품목 수", example = "2")
        long materialShortageCount,

        @Schema(description = "전체 재고 등록 자재 대비 SHORTAGE 상태 자재 비율", example = "3.0")
        BigDecimal materialShortageRate,

        @Schema(description = "월간 납기 대상 주문 중 납기일 내 완료된 주문 비율", example = "98.0")
        BigDecimal orderAchievementRate,

        @Schema(description = "생산계획 시뮬레이션 기준 절약 시간. DAY 단위", example = "37.0")
        BigDecimal savedDelayDays,

        @Schema(description = "절약 시간 단위", example = "DAY")
        String savedDelayUnit,

        @Schema(description = "응답 생성 시각", example = "2026-06-06T10:30:00+09:00")
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
