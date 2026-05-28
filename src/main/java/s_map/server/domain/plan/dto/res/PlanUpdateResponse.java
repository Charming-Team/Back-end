package s_map.server.domain.plan.dto.res;

import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.order.entity.ProductionPlan;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
public class PlanUpdateResponse {

    private Long planId;
    private Long orderId;
    private Long productId;
    private Long lineId;
    private Long operatorId;
    private OffsetDateTime plannedStartAt;
    private OffsetDateTime plannedEndAt;
    private BigDecimal estimatedDurationHr;
    private Integer plannedQuantity;
    private Integer planSequence;
    private String planStatus;
    private boolean applied;
    private String message;

    public static PlanUpdateResponse from(ProductionPlan plan) {
        return PlanUpdateResponse.builder()
                .planId(plan.getPlanId())
                .orderId(plan.getOrderId())
                .productId(plan.getProductId())
                .lineId(plan.getLineId())
                .operatorId(plan.getOperatorId())
                .plannedStartAt(plan.getPlannedStartAt())
                .plannedEndAt(plan.getPlannedEndAt())
                .estimatedDurationHr(plan.getEstimatedDurationHr())
                .plannedQuantity(plan.getPlannedQuantity())
                .planSequence(plan.getPlanSequence())
                .planStatus(plan.getPlanStatus().name())
                .applied(true)
                .build();
    }

    public static PlanUpdateResponse validationOnly(ProductionPlan plan) {
        return PlanUpdateResponse.builder()
                .planId(plan.getPlanId())
                .orderId(plan.getOrderId())
                .productId(plan.getProductId())
                .lineId(plan.getLineId())
                .operatorId(plan.getOperatorId())
                .plannedStartAt(plan.getPlannedStartAt())
                .plannedEndAt(plan.getPlannedEndAt())
                .estimatedDurationHr(plan.getEstimatedDurationHr())
                .plannedQuantity(plan.getPlannedQuantity())
                .planSequence(plan.getPlanSequence())
                .planStatus(plan.getPlanStatus().name())
                .applied(false)
                .message("생산계획 수정 요청값 검증이 완료되었습니다. 실제 반영은 시뮬레이션 승인 플로우에서 처리됩니다.")
                .build();
    }
}
