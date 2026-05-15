package s_map.server.domain.plan.dto.res;

import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.plan.entity.ProductionPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PlanUpdateResponse {

    private Long planId;
    private Long orderId;
    private Long productId;
    private Long lineId;
    private Long operatorId;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private BigDecimal estimatedDurationHr;
    private Integer plannedQuantity;
    private Integer planSequence;
    private String planStatus;

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
                .build();
    }
}