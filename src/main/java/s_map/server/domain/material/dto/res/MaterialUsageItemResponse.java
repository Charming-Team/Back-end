package s_map.server.domain.material.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.material.entity.MaterialPlanStatus;
import s_map.server.domain.material.entity.ProductionPlanMaterial;

import java.math.BigDecimal;

@Schema(description = "생산계획별 자재 사용량 항목")
public record MaterialUsageItemResponse(
        @Schema(description = "생산계획 ID", example = "1001")
        Long planId,

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
