package s_map.server.domain.material.dto.res;

import s_map.server.domain.material.entity.Material;
import s_map.server.domain.material.entity.MaterialInventory;
import s_map.server.domain.material.entity.ProductionPlanMaterial;

import java.math.BigDecimal;
import java.util.List;

public record MaterialUsageResponse(
        Long materialId,
        String materialCode,
        String materialName,
        String unit,
        BigDecimal currentQuantity,
        BigDecimal availableQuantity,
        BigDecimal reservedQuantity,
        BigDecimal safetyStockQuantity,
        BigDecimal totalExpectedUsage,
        BigDecimal totalReservedQuantity,
        BigDecimal totalConsumedQuantity,
        BigDecimal totalShortageQuantity,
        List<MaterialUsageItemResponse> usages
) {

    public static MaterialUsageResponse from(
            Material material,
            MaterialInventory inventory,
            List<ProductionPlanMaterial> planMaterials
    ) {
        List<MaterialUsageItemResponse> usages = planMaterials.stream()
                .map(MaterialUsageItemResponse::from)
                .toList();

        BigDecimal totalExpectedUsage = planMaterials.stream()
                .map(planMaterial -> zeroIfNull(planMaterial.getRequiredQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReservedQuantity = planMaterials.stream()
                .map(planMaterial -> zeroIfNull(planMaterial.getReservedQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalConsumedQuantity = planMaterials.stream()
                .map(planMaterial -> zeroIfNull(planMaterial.getConsumedQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalShortageQuantity = planMaterials.stream()
                .map(planMaterial -> zeroIfNull(planMaterial.getShortageQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new MaterialUsageResponse(
                material.getMaterialId(),
                material.getMaterialCode(),
                material.getMaterialName(),
                material.getUnit(),
                inventory != null ? inventory.getCurrentQuantity() : BigDecimal.ZERO,
                inventory != null ? inventory.getAvailableQuantity() : BigDecimal.ZERO,
                inventory != null ? inventory.getReservedQuantity() : BigDecimal.ZERO,
                inventory != null ? inventory.getSafetyStockQuantity() : BigDecimal.ZERO,
                totalExpectedUsage,
                totalReservedQuantity,
                totalConsumedQuantity,
                totalShortageQuantity,
                usages
        );
    }

    private static BigDecimal zeroIfNull(BigDecimal quantity) {
        return quantity != null ? quantity : BigDecimal.ZERO;
    }
}
