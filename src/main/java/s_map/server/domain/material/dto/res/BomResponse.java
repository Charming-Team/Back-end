package s_map.server.domain.material.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.material.entity.Bom;
import s_map.server.domain.material.entity.Material;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "BOM 응답")
public record BomResponse(
        @Schema(description = "BOM ID", example = "1")
        Long bomId,

        @Schema(description = "제품 ID", example = "1")
        Long productId,

        @Schema(description = "자재 ID", example = "1")
        Long materialId,

        @Schema(description = "자재 코드", example = "RM-AL-001")
        String materialCode,

        @Schema(description = "자재명", example = "알루미늄 원자재")
        String materialName,

        @Schema(description = "자재 유형", example = "원자재")
        String materialType,

        @Schema(description = "BOM 소요량 단위", example = "KG")
        String unit,

        @Schema(description = "제품 1단위 생산에 필요한 자재 소요량", example = "2.5000")
        BigDecimal requiredQuantityPerUnit,

        @Schema(description = "생산 과정 손실률. 2%는 0.0200처럼 0~1 기준으로 표시합니다.", example = "0.0200")
        BigDecimal lossRate,

        @Schema(description = "BOM 마지막 수정 일시", example = "2026-05-15T08:00:00")
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
