package s_map.server.domain.material.dto.res;

import s_map.server.domain.material.entity.InventoryStatus;
import s_map.server.domain.material.entity.Material;
import s_map.server.domain.material.entity.MaterialInventory;

import java.math.BigDecimal;

public record MaterialResponse(
        Long materialId,
        String materialCode,
        String materialName,
        String materialType,
        String unit,
        String description,
        BigDecimal currentQuantity,
        BigDecimal availableQuantity,
        BigDecimal reservedQuantity,
        BigDecimal safetyStockQuantity,
        InventoryStatus inventoryStatus
) {

    public static MaterialResponse from(Material material, MaterialInventory inventory) {
        return new MaterialResponse(
                material.getMaterialId(),
                material.getMaterialCode(),
                material.getMaterialName(),
                material.getMaterialType(),
                material.getUnit(),
                material.getDescription(),
                inventory != null ? inventory.getCurrentQuantity() : BigDecimal.ZERO,
                inventory != null ? inventory.getAvailableQuantity() : BigDecimal.ZERO,
                inventory != null ? inventory.getReservedQuantity() : BigDecimal.ZERO,
                inventory != null ? inventory.getSafetyStockQuantity() : BigDecimal.ZERO,
                inventory != null ? inventory.getInventoryStatus() : null
        );
    }
}