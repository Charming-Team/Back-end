package s_map.server.domain.plan.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.material.entity.ProductionPlanMaterial;

import java.math.BigDecimal;

@Schema(description = "생산계획별 필요 자재 응답")
@Getter
@Builder
public class PlanMaterialResponse {

    @Schema(description = "생산계획 자재 ID", example = "1")
    private Long planMaterialId;
    @Schema(description = "자재 ID", example = "5")
    private Long materialId;
    @Schema(description = "자재명", example = "ABS 원재료")
    private String materialName;
    @Schema(description = "자재 코드", example = "RM-ABS-001")
    private String materialCode;
    @Schema(description = "자재 단위", example = "KG")
    private String unit;
    @Schema(description = "필요 수량", example = "1200.00")
    private BigDecimal requiredQuantity;
    @Schema(description = "예약 수량", example = "1000.00")
    private BigDecimal reservedQuantity;
    @Schema(description = "사용 수량", example = "200.00")
    private BigDecimal consumedQuantity;
    @Schema(description = "부족 수량", example = "200.00")
    private BigDecimal shortageQuantity;
    @Schema(description = "자재 계획 상태 코드", example = "SHORTAGE")
    private String materialPlanStatus;
    @Schema(description = "자재 부족 여부", example = "true")
    private boolean shortage;

    public static PlanMaterialResponse from(ProductionPlanMaterial planMaterial) {
        return PlanMaterialResponse.builder()
                .planMaterialId(planMaterial.getPlanMaterialId())
                .materialId(planMaterial.getMaterial().getMaterialId())
                .materialName(planMaterial.getMaterial().getMaterialName())
                .materialCode(planMaterial.getMaterial().getMaterialCode())
                .unit(planMaterial.getMaterial().getUnit())
                .requiredQuantity(planMaterial.getRequiredQuantity())
                .reservedQuantity(planMaterial.getReservedQuantity())
                .consumedQuantity(planMaterial.getConsumedQuantity())
                .shortageQuantity(planMaterial.getShortageQuantity())
                .materialPlanStatus(planMaterial.getMaterialPlanStatus().name())
                .shortage(planMaterial.hasShortage())
                .build();
    }
}
