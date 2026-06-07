package s_map.server.domain.plan.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.order.entity.ProductionPlan;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "생산계획 수정 요청 검증 응답")
@Getter
@Builder
public class PlanUpdateResponse {

    @Schema(description = "생산계획 ID", example = "1")
    private Long planId;
    @Schema(description = "주문 ID", example = "10")
    private Long orderId;
    @Schema(description = "제품 ID", example = "3")
    private Long productId;
    @Schema(description = "생산 라인 ID", example = "2")
    private Long lineId;
    @Schema(description = "생산 담당자 ID", example = "12", nullable = true)
    private Long operatorId;
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
    @Schema(description = "실제 생산계획 반영 여부. 현재 검증 전용 응답은 false입니다.", example = "false")
    private boolean applied;
    @Schema(
            description = "검증 결과 안내 메시지",
            example = "생산계획 수정 요청값 검증이 완료되었습니다. 실제 반영은 시뮬레이션 승인 플로우에서 처리됩니다.",
            nullable = true
    )
    private String message;

    public static PlanUpdateResponse from(ProductionPlan plan) {
        return PlanUpdateResponse.builder()
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
                .applied(true)
                .build();
    }

    public static PlanUpdateResponse validationOnly(ProductionPlan plan) {
        return PlanUpdateResponse.builder()
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
                .applied(false)
                .message("생산계획 수정 요청값 검증이 완료되었습니다. 실제 반영은 시뮬레이션 승인 플로우에서 처리됩니다.")
                .build();
    }
}
