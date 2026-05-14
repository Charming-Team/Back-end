package s_map.server.domain.material.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.material.entity.InventoryStatus;
import s_map.server.domain.material.entity.Material;
import s_map.server.domain.material.entity.MaterialInventory;

import java.math.BigDecimal;

@Schema(description = "자재 목록 응답")
public record MaterialResponse(
        @Schema(description = "자재 ID", example = "1")
        Long materialId,

        @Schema(description = "자재 코드", example = "RM-AL-001")
        String materialCode,

        @Schema(description = "자재명", example = "알루미늄 원자재")
        String materialName,

        @Schema(description = "자재 유형", example = "원자재")
        String materialType,

        @Schema(description = "자재 단위", example = "KG")
        String unit,

        @Schema(description = "자재 설명 또는 비고", example = "배터리 모듈 하우징 생산에 사용하는 알루미늄 원자재")
        String description,

        @Schema(description = "현재 보유 중인 전체 재고량", example = "120.0000")
        BigDecimal currentQuantity,

        @Schema(description = "실제 생산에 사용 가능한 재고량", example = "90.0000")
        BigDecimal availableQuantity,

        @Schema(description = "생산계획에 이미 예약된 재고량", example = "30.0000")
        BigDecimal reservedQuantity,

        @Schema(description = "안전 재고 수량", example = "20.0000")
        BigDecimal safetyStockQuantity,

        @Schema(description = "재고 정보 등록 여부", example = "true")
        boolean inventoryRegistered,

        @Schema(description = "재고 상태", example = "NORMAL", allowableValues = {"NORMAL", "LOW", "SHORTAGE", "INBOUND_WAITING"})
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
                inventory != null,
                inventory != null ? inventory.getInventoryStatus() : null
        );
    }
}
