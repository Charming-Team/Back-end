package s_map.server.domain.plan.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.material.entity.ProductionPlanMaterial;
import s_map.server.domain.order.entity.PlanStatus;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.plan.repository.PlanRow;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Schema(description = "생산계획 상세 조회 응답")
@Getter
@Builder
public class PlanDetailResponse {

    @Schema(description = "생산계획 ID", example = "1")
    private Long planId;
    @Schema(description = "주문 ID", example = "10")
    private Long orderId;
    @Schema(description = "제품 ID", example = "3")
    private Long productId;
    @Schema(description = "제품 코드", example = "PRD-ABS-001", nullable = true)
    private String productCode;
    @Schema(description = "제품명", example = "자동차 브레이크 패드")
    private String productName;
    @Schema(description = "생산 라인 ID", example = "2")
    private Long lineId;
    @Schema(description = "생산 라인 코드", example = "LINE-A")
    private String lineCode;
    @Schema(description = "생산 라인명", example = "LINE-A")
    private String lineName;
    @Schema(description = "생산 담당자 ID", example = "12", nullable = true)
    private Long operatorId;
    @Schema(description = "생산 담당자명", example = "이수진", nullable = true)
    private String operatorName;
    @Schema(description = "계획 시작 일시", example = "2026-06-05T09:00:00+09:00")
    private OffsetDateTime plannedStartAt;
    @Schema(description = "계획 종료 일시", example = "2026-06-05T17:00:00+09:00")
    private OffsetDateTime plannedEndAt;
    @Schema(description = "예상 소요 시간(시간)", example = "8.00")
    private BigDecimal estimatedDurationHr;
    @Schema(description = "계획 생산 수량", example = "5000")
    private Integer plannedQuantity;
    @Schema(description = "라인 내 생산 순서", example = "3")
    private Integer planSequence;
    @Schema(
            description = "생산계획 상태 코드",
            example = "SCHEDULED",
            allowableValues = {"SCHEDULED", "IN_PROGRESS", "COMPLETED", "DELAYED", "CANCELLED"}
    )
    private String planStatus;
    @Schema(description = "생산계획 상태 한글 표시", example = "예정")
    private String planStatusLabel;
    @Schema(description = "생산계획 생성 일시", example = "2026-06-01T09:00:00+09:00")
    private OffsetDateTime createdAt;
    @Schema(description = "생산계획 수정 일시", example = "2026-06-02T10:00:00+09:00")
    private OffsetDateTime updatedAt;
    @Schema(description = "생산계획별 필요 자재 목록")
    private List<PlanMaterialResponse> materials;

    public static PlanDetailResponse of(
            ProductionPlan plan,
            List<ProductionPlanMaterial> planMaterials
    ) {
        return PlanDetailResponse.builder()
                .planId(plan.getPlanId())
                .orderId(plan.getOrderId())
                .productId(plan.getProductId())
                .lineId(plan.getLineId())
                .operatorId(plan.getOperatorId())
                .plannedStartAt(plan.getPlannedStartAt())
                .plannedEndAt(plan.getPlannedEndAt())
                .estimatedDurationHr(plan.getEstimatedDurationHr())
                .plannedQuantity(plan.getPlannedQuantity())
                .planSequence(plan.getPlanSequence())
                .planStatus(plan.getPlanStatus().name())
                .planStatusLabel(plan.getPlanStatus().getLabel())
                .createdAt(toOffsetDateTime(plan.getCreatedAt()))
                .updatedAt(toOffsetDateTime(plan.getUpdatedAt()))
                .materials(
                        planMaterials.stream()
                                .map(PlanMaterialResponse::from)
                                .toList()
                )
                .build();
    }

    public static PlanDetailResponse of(
            PlanRow row,
            List<ProductionPlanMaterial> planMaterials
    ) {
        return PlanDetailResponse.builder()
                .planId(row.planId())
                .orderId(row.orderId())
                .productId(row.productId())
                .productCode(row.productCode())
                .productName(row.productName())
                .lineId(row.lineId())
                .lineCode(row.lineCode())
                .lineName(row.lineName())
                .operatorId(row.operatorId())
                .operatorName(row.operatorName())
                .plannedStartAt(row.plannedStartAt())
                .plannedEndAt(row.plannedEndAt())
                .estimatedDurationHr(row.estimatedDurationHr())
                .plannedQuantity(row.plannedQuantity())
                .planSequence(row.planSequence())
                .planStatus(row.planStatus())
                .planStatusLabel(resolveStatusLabel(row.planStatus()))
                .createdAt(row.createdAt())
                .updatedAt(row.updatedAt())
                .materials(
                        planMaterials.stream()
                                .map(PlanMaterialResponse::from)
                                .toList()
                )
                .build();
    }

    private static String resolveStatusLabel(String status) {
        if (status == null) {
            return "확인 필요";
        }

        try {
            return PlanStatus.valueOf(status).getLabel();
        } catch (IllegalArgumentException ignored) {
            return "확인 필요";
        }
    }

    private static OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value != null ? value.atZone(ZoneId.of("Asia/Seoul")).toOffsetDateTime() : null;
    }
}
