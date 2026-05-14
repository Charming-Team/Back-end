package s_map.server.domain.material.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.material.entity.Material;
import s_map.server.domain.material.entity.MaterialPlanStatus;
import s_map.server.domain.material.entity.ProductionPlanMaterial;

import java.math.BigDecimal;

@Schema(description = "부족 자재 목록 응답")
public record MaterialShortageResponse(
        @Schema(description = "생산계획별 자재 소요 ID", example = "1")
        Long planMaterialId,

        @Schema(description = "생산계획 ID", example = "1001")
        Long planId,

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

        @Schema(description = "생산계획 수행에 필요한 총 자재 수량", example = "150.0000")
        BigDecimal requiredQuantity,

        @Schema(description = "생산계획에 예약된 자재 수량", example = "90.0000")
        BigDecimal reservedQuantity,

        @Schema(description = "실제 사용된 자재 수량", example = "0.0000")
        BigDecimal consumedQuantity,

        @Schema(description = "부족 수량", example = "60.0000")
        BigDecimal shortageQuantity,

        @Schema(description = "생산계획별 자재 상태", example = "SHORTAGE", allowableValues = {"READY", "PARTIAL_RESERVED", "SHORTAGE", "CONSUMED", "CANCELLED"})
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
