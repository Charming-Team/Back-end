package s_map.server.domain.order.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.order.entity.OrderStatus;
import s_map.server.domain.order.repository.OrderDetailProjection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "주문 상세 응답")
public record OrderDetailResponse(

        Long orderId,
        String orderNo,

        Long productId,
        String productCode,
        String productName,
        String productCategory,
        String productUnit,

        Integer orderQuantity,
        String customerName,
        String customerContactName,

        LocalDate orderDate,
        LocalDate dueDate,

        BigDecimal contractAmount,
        BigDecimal latePenaltyAmount,

        OrderStatus orderStatus,
        String orderStatusLabel,

        @Schema(description = "생산 계획상 라인 내 생산 순서. 실제 대응 우선순위가 아닙니다.")
        Integer planSequence,

        OffsetDateTime plannedStartAt,
        OffsetDateTime plannedEndAt,
        BigDecimal estimatedDurationHr,

        String lineNames,
        String operatorNames,

        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static OrderDetailResponse from(OrderDetailProjection projection) {
        OrderStatus status = OrderStatus.valueOf(projection.getOrderStatus());

        return new OrderDetailResponse(
                projection.getOrderId(),
                projection.getOrderNo(),
                projection.getProductId(),
                projection.getProductCode(),
                projection.getProductName(),
                projection.getProductCategory(),
                projection.getProductUnit(),
                projection.getOrderQuantity(),
                projection.getCustomerName(),
                projection.getCustomerContactName(),
                projection.getOrderDate(),
                projection.getDueDate(),
                projection.getContractAmount(),
                projection.getLatePenaltyAmount(),
                status,
                status.getLabel(),
                projection.getPlanSequence(),
                projection.getPlannedStartAt(),
                projection.getPlannedEndAt(),
                projection.getEstimatedDurationHr(),
                projection.getLineNames(),
                projection.getOperatorNames(),
                projection.getCreatedAt(),
                projection.getUpdatedAt()
        );
    }
}