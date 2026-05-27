package s_map.server.domain.order.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.order.entity.OrderStatus;
import s_map.server.domain.order.repository.OrderSummaryProjection;

import java.time.LocalDate;

@Schema(description = "주문 목록 응답")
public record OrderListResponse(

        @Schema(description = "주문 ID", example = "1")
        Long orderId,

        @Schema(description = "주문 번호", example = "PO-240520-001")
        String orderNo,

        @Schema(description = "고객사명", example = "A사")
        String customerName,

        @Schema(description = "제품 ID", example = "1")
        Long productId,

        @Schema(description = "제품 코드", example = "ABS-BLK")
        String productCode,

        @Schema(description = "제품명", example = "ABS-Black")
        String productName,

        @Schema(description = "주문 수량", example = "1000")
        Integer orderQuantity,

        @Schema(description = "납기일", example = "2026-06-21")
        LocalDate dueDate,

        @Schema(description = "주문 상태", example = "IN_PROGRESS")
        OrderStatus orderStatus,

        @Schema(description = "주문 상태 한글 표시", example = "진행 중")
        String orderStatusLabel
) {
    public static OrderListResponse from(OrderSummaryProjection projection) {
        OrderStatus status = OrderStatus.valueOf(projection.getOrderStatus());

        return new OrderListResponse(
                projection.getOrderId(),
                projection.getOrderNo(),
                projection.getCustomerName(),
                projection.getProductId(),
                projection.getProductCode(),
                projection.getProductName(),
                projection.getOrderQuantity(),
                projection.getDueDate(),
                status,
                status.getLabel()
        );
    }
}