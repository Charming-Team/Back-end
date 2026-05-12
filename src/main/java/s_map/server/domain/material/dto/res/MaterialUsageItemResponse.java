package s_map.server.domain.material.dto.res;

import s_map.server.domain.material.entity.MaterialPlanStatus;
import s_map.server.domain.material.entity.ProductionPlanMaterial;

import java.math.BigDecimal;

public record MaterialUsageItemResponse(
        Long planId,
        BigDecimal requiredQuantity,
        BigDecimal reservedQuantity,
        BigDecimal consumedQuantity,
        BigDecimal shortageQuantity,
        MaterialPlanStatus materialPlanStatus
) {

    public static MaterialUsageItemResponse from(ProductionPlanMaterial planMaterial) {
        return new MaterialUsageItemResponse(
                planMaterial.getPlanId(),
                planMaterial.getRequiredQuantity(),
                planMaterial.getReservedQuantity(),
                planMaterial.getConsumedQuantity(),
                planMaterial.getShortageQuantity(),
                planMaterial.getMaterialPlanStatus()
        );
    }
}