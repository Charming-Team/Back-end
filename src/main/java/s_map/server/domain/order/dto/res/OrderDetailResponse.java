package s_map.server.domain.order.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.order.entity.OrderStatus;
import s_map.server.domain.order.repository.OrderDetailProjection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

        @Schema(description = "납기일과 상태 기준 우선 대응 순위. 완료/취소 주문은 null입니다.")
        Integer priorityRank,
        String priorityMessage,

        @Schema(description = "생산 계획상 라인 내 생산 순서. 실제 대응 우선순위가 아닙니다.")
        Integer planSequence,

        OffsetDateTime plannedStartAt,
        OffsetDateTime plannedEndAt,
        BigDecimal estimatedDurationHr,

        String lineNames,
        String operatorNames,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
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
                projection.getPriorityRank(),
                createPriorityMessage(projection.getPriorityRank(), status),
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

    private static String createPriorityMessage(Integer priorityRank, OrderStatus status) {
        if (priorityRank == null || status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            return null;
        }

        if (status == OrderStatus.DELAYED) {
            return "지연된 주문입니다.";
        }

        if (priorityRank <= 3) {
            return "우선 대응이 필요한 주문입니다.";
        }

        return "일정에 맞춰 관리가 필요한 주문입니다.";
    }
}
