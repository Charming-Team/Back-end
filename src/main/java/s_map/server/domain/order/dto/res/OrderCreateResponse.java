package s_map.server.domain.order.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.order.entity.CustomerOrder;
import s_map.server.domain.order.entity.ProductionPlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "주문 생성 응답")
public record OrderCreateResponse(

        Long orderId,
        String orderNo,
        Long productId,
        String productName,
        Integer orderQuantity,
        String customerName,
        String customerContactName,
        LocalDate dueDate,
        String orderStatus,
        String orderStatusLabel,

        Long planId,
        Long lineId,
        String lineName,
        Long operatorId,
        OffsetDateTime plannedStartAt,
        OffsetDateTime plannedEndAt,
        BigDecimal estimatedDurationHr,
        Integer planSequence,
        String planStatus,
        String planStatusLabel
) {
    public static OrderCreateResponse from(
            CustomerOrder order,
            String productName,
            ProductionPlan plan,
            String lineName
    ) {
        return new OrderCreateResponse(
                order.getOrderId(),
                order.getOrderNo(),
                order.getProductId(),
                productName,
                order.getOrderQuantity(),
                order.getCustomerName(),
                order.getCustomerContactName(),
                order.getDueDate(),
                order.getOrderStatus().name(),
                order.getOrderStatus().getLabel(),
                plan.getPlanId(),
                plan.getLineId(),
                lineName,
                plan.getOperatorId(),
                plan.getPlannedStartAt(),
                plan.getPlannedEndAt(),
                plan.getEstimatedDurationHr(),
                plan.getPlanSequence(),
                plan.getPlanStatus().name(),
                plan.getPlanStatus().getLabel()
        );
    }
}