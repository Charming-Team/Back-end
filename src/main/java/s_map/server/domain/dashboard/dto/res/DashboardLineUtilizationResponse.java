package s_map.server.domain.dashboard.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.dashboard.repository.DashboardRepository.LineUtilizationRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Schema(description = "대시보드 라인별 가동 현황 응답")
public record DashboardLineUtilizationResponse(
        @Schema(description = "활성 라인별 최신 가동 현황 목록")
        List<LineUtilizationItem> lines
) {

    public static DashboardLineUtilizationResponse from(List<LineUtilizationRow> rows) {
        return new DashboardLineUtilizationResponse(
                rows.stream()
                        .map(LineUtilizationItem::from)
                        .toList()
        );
    }

    public record LineUtilizationItem(
            @Schema(description = "라인 ID", example = "1")
            Long lineId,

            @Schema(description = "라인명", example = "Line A")
            String lineName,

            @Schema(description = "가동률 퍼센트", example = "80.0")
            BigDecimal utilizationRate,

            @Schema(description = "최신 라인 가동 상태. 상태 데이터가 없으면 null", example = "RUNNING", nullable = true)
            String operationStatus,

            @Schema(description = "라인 가동 상태 한글 표시", example = "가동 중", allowableValues = {"가동 중", "가동 지연", "상태 확인 필요"})
            String displayStatus
    ) {

        public static LineUtilizationItem from(LineUtilizationRow row) {
            BigDecimal percent = toPercent(row.utilizationRate());

            return new LineUtilizationItem(
                    row.lineId(),
                    row.lineName(),
                    percent,
                    row.operationStatus(),
                    toDisplayStatus(row.operationStatus(), percent)
            );
        }

        private static BigDecimal toPercent(BigDecimal value) {
            if (value == null) {
                return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
            }

            return value.multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP);
        }

        private static String toDisplayStatus(String operationStatus, BigDecimal utilizationRate) {
            if (operationStatus == null || operationStatus.isBlank()) {
                return "상태 확인 필요";
            }

            if ("ERROR".equals(operationStatus)
                    || "STOPPED".equals(operationStatus)
                    || "MAINTENANCE".equals(operationStatus)) {
                return "가동 지연";
            }

            if ("SETUP".equals(operationStatus)) {
                return "가동 지연";
            }

            if ("IDLE".equals(operationStatus)) {
                return "가동 지연";
            }

            if (utilizationRate.compareTo(BigDecimal.valueOf(50)) < 0) {
                return "가동 지연";
            }

            if ("RUNNING".equals(operationStatus)) {
                return "가동 중";
            }

            return "상태 확인 필요";
        }
    }
}
