package s_map.server.domain.material.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.material.entity.Material;
import s_map.server.domain.material.entity.MaterialInventory;
import s_map.server.domain.material.entity.ProductionPlanMaterial;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "자재 사용량 조회 응답")
public record MaterialUsageResponse(
        @Schema(description = "자재 ID", example = "1")
        Long materialId,

        @Schema(description = "자재 코드", example = "RM-AL-001")
        String materialCode,

        @Schema(description = "자재명", example = "알루미늄 원자재")
        String materialName,

        @Schema(description = "자재 단위", example = "KG")
        String unit,

        @Schema(description = "현재 보유 중인 전체 재고량", example = "120.0000")
        BigDecimal currentQuantity,

        @Schema(description = "실제 생산에 사용 가능한 재고량", example = "90.0000")
        BigDecimal availableQuantity,

        @Schema(description = "생산계획에 이미 예약된 재고량", example = "30.0000")
        BigDecimal reservedQuantity,

        @Schema(description = "안전 재고 수량", example = "20.0000")
        BigDecimal safetyStockQuantity,

        @Schema(description = "전체 생산계획 기준 예상 필요 수량 합계", example = "250.0000")
        BigDecimal totalExpectedUsage,

        @Schema(description = "전체 생산계획 기준 예약 수량 합계", example = "180.0000")
        BigDecimal totalReservedQuantity,

        @Schema(description = "전체 생산계획 기준 실제 사용 수량 합계", example = "50.0000")
        BigDecimal totalConsumedQuantity,

        @Schema(description = "전체 생산계획 기준 부족 수량 합계", example = "70.0000")
        BigDecimal totalShortageQuantity,

        @Schema(description = "생산계획별 자재 사용량 목록")
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
