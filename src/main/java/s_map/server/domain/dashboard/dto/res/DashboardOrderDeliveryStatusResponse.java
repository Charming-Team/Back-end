package s_map.server.domain.dashboard.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.dashboard.repository.DashboardRepository.OrderDeliveryStatusRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "대시보드 주문 및 납기 현황 응답")
public record DashboardOrderDeliveryStatusResponse(
        @Schema(description = "현재 진행 중 또는 지연 상태인 전체 주문의 평균 진행률", example = "58.0")
        BigDecimal averageProgressRate,

        @Schema(description = "화면에 표시할 주문 및 납기 현황 목록")
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
            @Schema(description = "주문 ID", example = "1")
            Long orderId,

            @Schema(description = "주문 번호", example = "PO-240520-001")
            String orderNo,

            @Schema(description = "납기일", example = "2026-06-21")
            LocalDate dueDate,

            @Schema(description = "주문 수량", example = "1000")
            Integer orderQuantity,

            @Schema(description = "실제 생산 수량", example = "720")
            Integer actualQuantity,

            @Schema(description = "주문 진행률. 실제 생산 수량 / 주문 수량 기준 퍼센트", example = "72.0")
            BigDecimal progressRate,

            @Schema(description = "현재 시점 기준 주문 상태", example = "IN_PROGRESS")
            String orderStatus,

            @Schema(description = "주문 상태 한글 표시", example = "진행 중", allowableValues = {"진행 중", "지연", "완료", "대기", "취소", "확인 필요"})
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
