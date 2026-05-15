package s_map.server.domain.plan.dto.res;

import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.material.entity.ProductionPlanMaterial;
import s_map.server.domain.plan.entity.ProductionPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PlanDetailResponse {

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
    private List<PlanMaterialResponse> materials;

    public static PlanDetailResponse of(
            ProductionPlan plan,
            List<ProductionPlanMaterial> planMaterials
    ) {
        return PlanDetailResponse.builder()
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
                .materials(
                        planMaterials.stream()
                                .map(PlanMaterialResponse::from)
                                .toList()
                )
                .build();
    }
}