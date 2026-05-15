package s_map.server.domain.plan.dto.res;

import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.material.entity.ProductionPlanMaterial;

import java.math.BigDecimal;

@Getter
@Builder
public class PlanMaterialResponse {

    private Long planMaterialId;
    private Long materialId;
    private String materialName;
    private String materialCode;
    private String unit;
    private BigDecimal requiredQuantity;
    private BigDecimal reservedQuantity;
    private BigDecimal consumedQuantity;
    private BigDecimal shortageQuantity;
    private String materialPlanStatus;
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
