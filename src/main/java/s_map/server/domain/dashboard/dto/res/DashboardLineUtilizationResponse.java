package s_map.server.domain.dashboard.dto.res;

import s_map.server.domain.dashboard.repository.DashboardRepository.LineUtilizationRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record DashboardLineUtilizationResponse(
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
            Long lineId,
            String lineName,
            BigDecimal utilizationRate,
            String operationStatus,
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
            if ("ERROR".equals(operationStatus)
                    || "STOPPED".equals(operationStatus)
                    || "MAINTENANCE".equals(operationStatus)) {
                return "비가동";
            }

            if ("SETUP".equals(operationStatus)) {
                return "셋업";
            }

            if ("IDLE".equals(operationStatus)) {
                return "대기";
            }

            // 화면상 36%, 42%가 가동 저조로 표시되어 있어 50% 미만을 임시 기준으로 둠.
            // 팀 기준이 따로 생기면 이 값은 수정 필요.
            if (utilizationRate.compareTo(BigDecimal.valueOf(50)) < 0) {
                return "가동 저조";
            }

            if ("RUNNING".equals(operationStatus)) {
                return "가동 중";
            }

            return "확인 필요";
        }
    }
}