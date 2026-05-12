package s_map.server.domain.material.dto.res;

import s_map.server.domain.material.entity.InventoryStatus;
import s_map.server.domain.material.entity.Material;
import s_map.server.domain.material.entity.MaterialInventory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MaterialDetailResponse(
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
        LocalDateTime expectedInboundAt,
        BigDecimal expectedInboundQuantity,
        InventoryStatus inventoryStatus,
        LocalDateTime updatedAt
) {

    public static MaterialDetailResponse from(Material material, MaterialInventory inventory) {
        return new MaterialDetailResponse(
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
                inventory != null ? inventory.getExpectedInboundAt() : null,
                inventory != null ? inventory.getExpectedInboundQuantity() : null,
                inventory != null ? inventory.getInventoryStatus() : null,
                material.getUpdatedAt()
        );
    }
}