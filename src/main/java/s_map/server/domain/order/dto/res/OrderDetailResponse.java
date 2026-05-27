package s_map.server.domain.order.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.order.entity.OrderStatus;
import s_map.server.domain.order.repository.OrderDetailProjection;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Schema(description = "주문 상세 응답")
public record OrderDetailResponse(

        @Schema(description = "주문 ID", example = "1")
        Long orderId,

        @Schema(description = "주문번호", example = "PO-260528-001")
        String orderNo,

        @Schema(description = "제품 ID", example = "1")
        Long productId,

        @Schema(description = "제품 코드", example = "ABS-BLK")
        String productCode,

        @Schema(description = "제품명", example = "ABS-Black")
        String productName,

        @Schema(description = "제품 카테고리", example = "ABS")
        String productCategory,

        @Schema(description = "제품 단위", example = "KG")
        String productUnit,

        @Schema(description = "주문 수량", example = "1000")
        Integer orderQuantity,

        @Schema(description = "고객사명", example = "A사")
        String customerName,

        @Schema(description = "고객사 담당자명", example = "배난수")
        String customerContactName,

        @Schema(description = "주문일", example = "2026-05-28")
        LocalDate orderDate,

        @Schema(description = "납기일", example = "2026-06-30")
        LocalDate dueDate,

        @Schema(description = "계약 금액", example = "10000000.00")
        BigDecimal contractAmount,

        @Schema(description = "지체상금 금액", example = "500000.00")
        BigDecimal latePenaltyAmount,

        @Schema(description = "주문 상태", example = "IN_PROGRESS")
        OrderStatus orderStatus,

        @Schema(description = "주문 상태 한글 표시", example = "진행 중")
        String orderStatusLabel,

        @Schema(description = "납기일과 상태 기준 우선 대응 순위. 완료/취소 주문은 null입니다.", example = "1")
        Integer priorityRank,

        @Schema(description = "우선순위 안내 메시지. 완료/취소 주문은 null입니다.", example = "우선 대응이 필요한 주문입니다.")
        String priorityMessage,

        @Schema(description = "생산 계획상 라인 내 생산 순서. 실제 대응 우선순위가 아닙니다.", example = "3")
        Integer planSequence,

        @Schema(description = "계획 시작 일시", example = "2026-05-28T09:00:00+09:00")
        OffsetDateTime plannedStartAt,

        @Schema(description = "계획 종료 일시", example = "2026-05-29T09:00:00+09:00")
        OffsetDateTime plannedEndAt,

        @Schema(description = "예상 소요 시간(시간)", example = "24.00")
        BigDecimal estimatedDurationHr,

        @Schema(description = "배정된 생산 라인명 목록", example = "ABS 주 생산 Line")
        String lineNames,

        @Schema(description = "배정된 작업자명 목록", example = "신작업")
        String operatorNames,

        @Schema(description = "주문 생성 일시", example = "2026-05-28T10:15:30+09:00")
        OffsetDateTime createdAt,

        @Schema(description = "주문 수정 일시", example = "2026-05-28T10:15:30+09:00")
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
                projection.getPriorityRank(),
                createPriorityMessage(projection.getPriorityRank(), status),
                projection.getPlanSequence(),
                toKst(projection.getPlannedStartAt()),
                toKst(projection.getPlannedEndAt()),
                projection.getEstimatedDurationHr(),
                projection.getLineNames(),
                projection.getOperatorNames(),
                toKst(projection.getCreatedAt()),
                toKst(projection.getUpdatedAt())
        );
    }

    private static OffsetDateTime toKst(Instant instant) {
        if (instant == null) {
            return null;
        }

        return instant.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime();
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
