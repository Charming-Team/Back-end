package s_map.server.domain.material.dto.res;

import s_map.server.domain.material.entity.Bom;
import s_map.server.domain.material.entity.Material;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BomResponse(
        Long bomId,
        Long productId,
        Long materialId,
        String materialCode,
        String materialName,
        String materialType,
        String unit,
        BigDecimal requiredQuantityPerUnit,
        BigDecimal lossRate,
        LocalDateTime updatedAt
) {

    public static BomResponse from(Bom bom) {
        Material material = bom.getMaterial();

        return new BomResponse(
                bom.getBomId(),
                bom.getProductId(),
                material.getMaterialId(),
                material.getMaterialCode(),
                material.getMaterialName(),
                material.getMaterialType(),
                bom.getUnit(),
                bom.getRequiredQuantityPerUnit(),
                bom.getLossRate(),
                bom.getUpdatedAt()
        );
    }
}