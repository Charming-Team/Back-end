package s_map.server.domain.order.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.order.entity.CustomerOrder;
import s_map.server.domain.order.entity.ProductionPlan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "주문 생성 응답")
public record OrderCreateResponse(

        @Schema(description = "생성된 주문 ID", example = "1")
        Long orderId,

        @Schema(description = "서버가 발급한 주문번호", example = "PO-260528-001")
        String orderNo,

        @Schema(description = "제품 ID", example = "1")
        Long productId,

        @Schema(description = "제품명", example = "ABS-Black")
        String productName,

        @Schema(description = "주문 수량", example = "1000")
        Integer orderQuantity,

        @Schema(description = "고객사명", example = "A사")
        String customerName,

        @Schema(description = "고객사 담당자명", example = "배난수")
        String customerContactName,

        @Schema(description = "납기일", example = "2026-06-30")
        LocalDate dueDate,

        @Schema(description = "주문 상태 코드", example = "WAITING")
        String orderStatus,

        @Schema(description = "주문 상태 한글 표시", example = "예정")
        String orderStatusLabel,

        @Schema(description = "자동 생성된 생산계획 ID", example = "10")
        Long planId,

        @Schema(description = "배정된 생산 라인 ID", example = "1")
        Long lineId,

        @Schema(description = "배정된 생산 라인명", example = "ABS 주 생산 Line")
        String lineName,

        @Schema(description = "생산 담당자 ID", example = "12")
        Long operatorId,

        @Schema(description = "계획 시작 일시", example = "2026-05-28T09:00:00+09:00")
        OffsetDateTime plannedStartAt,

        @Schema(description = "계획 종료 일시", example = "2026-05-29T09:00:00+09:00")
        OffsetDateTime plannedEndAt,

        @Schema(description = "예상 소요 시간(시간)", example = "24.00")
        BigDecimal estimatedDurationHr,

        @Schema(description = "배정 라인 내 생산 순서", example = "3")
        Integer planSequence,

        @Schema(description = "생산계획 상태 코드", example = "SCHEDULED")
        String planStatus,

        @Schema(description = "생산계획 상태 한글 표시", example = "예정")
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
