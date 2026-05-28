package s_map.server.domain.plan.dto.res;

import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.plan.repository.ProductionResultRow;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
public class CurrentPlanResponse {

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

    private OffsetDateTime actualStartAt;
    private OffsetDateTime actualEndAt;
    private BigDecimal actualQuantity;
    private BigDecimal defectQuantity;
    private BigDecimal yieldRate;

    public static CurrentPlanResponse of(
            ProductionPlan plan,
            ProductionResultRow result
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
                .actualStartAt(result != null ? result.actualStartAt() : null)
                .actualEndAt(result != null ? result.actualEndAt() : null)
                .actualQuantity(result != null ? result.actualQuantity() : BigDecimal.ZERO)
                .defectQuantity(result != null ? result.defectQuantity() : BigDecimal.ZERO)
                .yieldRate(result != null ? result.yieldRate() : null)
                .build();
    }
}
