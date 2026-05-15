package s_map.server.domain.plan.dto.res;

import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.plan.entity.ProductionPlan;
import s_map.server.domain.plan.entity.ProductionResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CurrentPlanResponse {

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

    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    private BigDecimal actualQuantity;
    private BigDecimal defectQuantity;
    private BigDecimal yieldRate;

    public static CurrentPlanResponse of(
            ProductionPlan plan,
            ProductionResult result
    ) {
        return CurrentPlanResponse.builder()
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
                .actualStartAt(result != null ? result.getActualStartAt() : null)
                .actualEndAt(result != null ? result.getActualEndAt() : null)
                .actualQuantity(result != null ? result.getActualQuantity() : BigDecimal.ZERO)
                .defectQuantity(result != null ? result.getDefectQuantity() : BigDecimal.ZERO)
                .yieldRate(result != null ? result.getYieldRate() : null)
                .build();
    }
}