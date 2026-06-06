package s_map.server.domain.dashboard.dto.res;

import s_map.server.domain.dashboard.repository.DashboardRepository.OrderDeliveryStatusRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

public record DashboardOrderDeliveryStatusResponse(
        BigDecimal averageProgressRate,
        List<OrderDeliveryStatusItem> orders
) {

    public static DashboardOrderDeliveryStatusResponse from(
            BigDecimal averageProgressRate,
            List<OrderDeliveryStatusRow> rows
    ) {
        List<OrderDeliveryStatusItem> orders = rows.stream()
                .map(OrderDeliveryStatusItem::from)
                .toList();

        return new DashboardOrderDeliveryStatusResponse(normalizeRate(averageProgressRate), orders);
    }

    private static BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return rate.setScale(1, RoundingMode.HALF_UP);
    }

    public record OrderDeliveryStatusItem(
            Long orderId,
            String orderNo,
            LocalDate dueDate,
            Integer orderQuantity,
            Integer actualQuantity,
            BigDecimal progressRate,
            String orderStatus,
            String displayStatus
    ) {

        public static OrderDeliveryStatusItem from(OrderDeliveryStatusRow row) {
            return new OrderDeliveryStatusItem(
                    row.orderId(),
                    row.orderNo(),
                    row.dueDate(),
                    row.orderQuantity(),
                    row.actualQuantity(),
                    calculateProgressRate(row.actualQuantity(), row.orderQuantity()),
                    row.orderStatus(),
                    toDisplayStatus(row.orderStatus())
            );
        }

        private static BigDecimal calculateProgressRate(Integer actualQuantity, Integer orderQuantity) {
            if (orderQuantity == null || orderQuantity <= 0) {
                return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
            }

            int safeActualQuantity = actualQuantity == null ? 0 : actualQuantity;

            return BigDecimal.valueOf(safeActualQuantity)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(orderQuantity), 1, RoundingMode.HALF_UP);
        }

        private static String toDisplayStatus(String orderStatus) {
            if ("DELAYED".equals(orderStatus)) {
                return "지연";
            }

            if ("IN_PROGRESS".equals(orderStatus)) {
                return "진행 중";
            }

            if ("COMPLETED".equals(orderStatus)) {
                return "완료";
            }

            if ("WAITING".equals(orderStatus)) {
                return "대기";
            }

            if ("CANCELLED".equals(orderStatus)) {
                return "취소";
            }

            return "확인 필요";
        }
    }
}
