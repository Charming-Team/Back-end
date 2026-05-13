package s_map.server.domain.material.dto.res;

import s_map.server.domain.material.entity.Material;
import s_map.server.domain.material.entity.MaterialPlanStatus;
import s_map.server.domain.material.entity.ProductionPlanMaterial;

import java.math.BigDecimal;

public record MaterialShortageResponse(
        Long planMaterialId,
        Long planId,
        Long materialId,
        String materialCode,
        String materialName,
        String materialType,
        String unit,
        BigDecimal requiredQuantity,
        BigDecimal reservedQuantity,
        BigDecimal consumedQuantity,
        BigDecimal shortageQuantity,
        MaterialPlanStatus materialPlanStatus
) {

    public static MaterialShortageResponse from(ProductionPlanMaterial planMaterial) {
        Material material = planMaterial.getMaterial();

        return new MaterialShortageResponse(
                planMaterial.getPlanMaterialId(),
                planMaterial.getPlanId(),
                material.getMaterialId(),
                material.getMaterialCode(),
                material.getMaterialName(),
                material.getMaterialType(),
                material.getUnit(),
                planMaterial.getRequiredQuantity(),
                planMaterial.getReservedQuantity(),
                planMaterial.getConsumedQuantity(),
                planMaterial.getShortageQuantity(),
                planMaterial.getMaterialPlanStatus()
        );
    }
}