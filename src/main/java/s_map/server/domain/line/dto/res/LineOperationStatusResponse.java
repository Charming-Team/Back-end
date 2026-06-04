package s_map.server.domain.line.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.line.entity.OperationStatus;
import s_map.server.domain.line.repository.LineOperationStatusProjection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Schema(description = "라인별 가동 현황 응답")
public record LineOperationStatusResponse(

        @Schema(description = "라인 ID", example = "1")
        Long lineId,

        @Schema(description = "라인 코드", example = "LINE-ABS-01")
        String lineCode,

        @Schema(description = "라인명", example = "ABS 주 생산 Line")
        String lineName,

        @Schema(description = "가동률. 0~1 기준", example = "0.8800")
        BigDecimal utilizationRate,

        @Schema(description = "가동률 퍼센트", example = "88")
        Integer utilizationRatePercent,

        @Schema(description = "현재 생산 제품 ID", example = "1")
        Long currentProductId,

        @Schema(description = "현재 생산 제품명", example = "ABS-BLACK")
        String currentProductName,

        @Schema(description = "다음 생산 제품 ID", example = "2")
        Long nextProductId,

        @Schema(description = "다음 생산 제품명", example = "ABS-WHITE")
        String nextProductName,

        @Schema(description = "라인 가동 상태", example = "RUNNING")
        OperationStatus operationStatus,

        @Schema(description = "라인 가동 상태 한글 표시", example = "가동 중")
        String operationStatusLabel,

        @Schema(description = "전환 예정 시각", example = "2026-06-05T15:00:00+09:00")
        OffsetDateTime transitionAt,

        @Schema(description = "전환 예정 시간 표시", example = "1.2h 후")
        String transitionExpectedTime,

        @Schema(description = "라인 상태 기록 시각", example = "2026-06-05T10:00:00+09:00")
        OffsetDateTime recordedAt
) {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");

    public static LineOperationStatusResponse from(
            LineOperationStatusProjection projection,
            OffsetDateTime now
    ) {
        OperationStatus status = toOperationStatus(projection.getOperationStatus());
        OffsetDateTime transitionAt = toKst(projection.getTransitionAt());

        return new LineOperationStatusResponse(
                projection.getLineId(),
                projection.getLineCode(),
                projection.getLineName(),
                projection.getUtilizationRate(),
                toPercent(projection.getUtilizationRate()),
                projection.getCurrentProductId(),
                projection.getCurrentProductName(),
                projection.getNextProductId(),
                projection.getNextProductName(),
                status,
                status != null ? status.getLabel() : "확인 필요",
                transitionAt,
                createTransitionExpectedTime(now, transitionAt),
                toKst(projection.getRecordedAt())
        );
    }

    private static OperationStatus toOperationStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return OperationStatus.valueOf(value);
    }

    private static Integer toPercent(BigDecimal rate) {
        if (rate == null) {
            return null;
        }

        return rate.multiply(BigDecimal.valueOf(100))
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
