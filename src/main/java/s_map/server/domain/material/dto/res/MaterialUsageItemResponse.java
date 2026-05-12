package s_map.server.domain.material.dto.res;

import s_map.server.domain.material.entity.MaterialPlanStatus;
import s_map.server.domain.material.entity.ProductionPlan;
import s_map.server.domain.material.entity.ProductionPlanMaterial;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MaterialUsageItemResponse(
        Long planId,
        Long orderId,
        Long productId,
        Long lineId,
        Long operatorId,
        LocalDateTime plannedStartAt,
        LocalDateTime plannedEndAt,
        Integer plannedQuantity,
        Integer planSequence,
        BigDecimal requiredQuantity,
        BigDecimal reservedQuantity,
        BigDecimal consumedQuantity,
        BigDecimal shortageQuantity,
        MaterialPlanStatus materialPlanStatus
) {

    public static MaterialUsageItemResponse from(ProductionPlanMaterial planMaterial) {
        ProductionPlan plan = planMaterial.getProductionPlan();

        return new MaterialUsageItemResponse(
                plan.getPlanId(),
                plan.getOrderId(),
                plan.getProductId(),
                plan.getLineId(),
                plan.getOperatorId(),
                plan.getPlannedStartAt(),
                plan.getPlannedEndAt(),
                plan.getPlannedQuantity(),
                plan.getPlanSequence(),
                planMaterial.getRequiredQuantity(),
                planMaterial.getReservedQuantity(),
                planMaterial.getConsumedQuantity(),
                planMaterial.getShortageQuantity(),
                planMaterial.getMaterialPlanStatus()
        );
    }
}