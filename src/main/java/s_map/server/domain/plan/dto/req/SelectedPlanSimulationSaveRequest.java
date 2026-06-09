package s_map.server.domain.plan.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import s_map.server.domain.order.entity.PlanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "사용자 선택 생산계획 시뮬레이션 저장 요청")
@Getter
public class SelectedPlanSimulationSaveRequest {

    @Schema(description = "시뮬레이션 그룹 ID. 전달하지 않으면 서버에서 생성합니다.", example = "SIM-GRP-20260609-001")
    private String simulationGroupId;

    @Schema(description = "시뮬레이션명", example = "납기 최적화 대안")
    @NotBlank(message = "시뮬레이션명은 필수입니다.")
    private String simulationName;

    @Schema(description = "연결된 AI 예측 결과 ID", example = "1", nullable = true)
    private Long predictionId;

    @Schema(
            description = "AI 대안 코드. DUE_DATE_OPTIMAL 또는 AMOUNT_OPTIMAL",
            example = "DUE_DATE_OPTIMAL"
    )
    @NotBlank(message = "AI 대안 코드는 필수입니다.")
    private String planVariantCode;

    @Schema(description = "시뮬레이션 실행 결과 요약", example = "납기 지연 위험 감소를 위한 라인 재배정")
    private String actionResult;

    @Schema(description = "변경 전 총 지연 시간(hr)", example = "36.50")
    @NotNull(message = "변경 전 총 지연 시간은 필수입니다.")
    private BigDecimal beforeTotalDelayHr;

    @Schema(description = "변경 후 총 지연 시간(hr)", example = "12.00")
    @NotNull(message = "변경 후 총 지연 시간은 필수입니다.")
    private BigDecimal afterTotalDelayHr;

    @Schema(description = "지연 감소 시간(hr)", example = "24.50")
    private BigDecimal delayReductionHr;

    @Schema(description = "변경 전 평균 라인 가동률. 0~1 사이 값", example = "0.7200", nullable = true)
    private BigDecimal beforeAvgLineUtilizationRate;

    @Schema(description = "변경 후 평균 라인 가동률. 0~1 사이 값", example = "0.8150", nullable = true)
    private BigDecimal afterAvgLineUtilizationRate;

    @Schema(description = "변경 전 총 생산 수량", example = "128000", nullable = true)
    private Integer beforeTotalProductionQuantity;

    @Schema(description = "변경 후 총 생산 수량", example = "132000", nullable = true)
    private Integer afterTotalProductionQuantity;

    @Schema(description = "변경 전 병목 라인 ID", example = "2", nullable = true)
    private Long beforeBottleneckLineId;

    @Schema(description = "변경 후 병목 라인 ID", example = "4", nullable = true)
    private Long afterBottleneckLineId;

    @Schema(description = "비용 변화 금액. 프론트에서 절대값 처리 후 전달합니다.", example = "1500000.00", nullable = true)
    private BigDecimal costChangeAmount;

    @Schema(description = "추천 등급", example = "HIGH", allowableValues = {"HIGH", "MEDIUM", "LOW"})
    private String recommendationGrade;

    @Schema(description = "사용자가 선택한 대안에 포함된 생산계획 목록")
    @NotEmpty(message = "저장할 생산계획 목록은 1건 이상이어야 합니다.")
    @Valid
    private List<SelectedPlan> plans;

    @Getter
    @Schema(description = "사용자 선택 대안의 생산계획")
    public static class SelectedPlan {

        @Schema(description = "주문 ID. customer_orders에 이미 존재해야 합니다.", example = "421")
        @NotNull(message = "주문 ID는 필수입니다.")
        private Long orderId;

        @Schema(description = "제품 ID", example = "2")
        @NotNull(message = "제품 ID는 필수입니다.")
        private Long productId;

        @Schema(description = "생산 라인 ID", example = "1")
        @NotNull(message = "생산 라인 ID는 필수입니다.")
        private Long lineId;

        @Schema(description = "생산 담당자 ID", example = "11", nullable = true)
        private Long operatorId;

        @Schema(description = "계획 시작 일시", example = "2026-05-01T09:00:00+09:00")
        @NotNull(message = "계획 시작 일시는 필수입니다.")
        private OffsetDateTime plannedStartAt;

        @Schema(description = "계획 종료 일시", example = "2026-05-02T14:51:00+09:00")
        @NotNull(message = "계획 종료 일시는 필수입니다.")
        private OffsetDateTime plannedEndAt;

        @Schema(description = "예상 소요 시간(hr)", example = "29.85")
        @NotNull(message = "예상 소요 시간은 필수입니다.")
        private BigDecimal estimatedDurationHr;

        @Schema(description = "계획 생산 수량", example = "18700")
        @NotNull(message = "계획 생산 수량은 필수입니다.")
        @Positive(message = "계획 생산 수량은 0보다 커야 합니다.")
        private Integer plannedQuantity;

        @Schema(description = "라인 내 생산 순서", example = "1")
        @NotNull(message = "라인 내 생산 순서는 필수입니다.")
        @Positive(message = "라인 내 생산 순서는 0보다 커야 합니다.")
        private Integer planSequence;

        @Schema(description = "생산계획 상태. 전달하지 않으면 SCHEDULED로 저장합니다.", example = "SCHEDULED")
        private PlanStatus planStatus;

        @Schema(description = "변경 전 생산 라인 ID", example = "2", nullable = true)
        private Long beforeLineId;

        @Schema(description = "변경 전 라인 내 생산 순서", example = "3", nullable = true)
        private Integer beforeSequence;

        @Schema(description = "변경 전 계획 시작 일시", example = "2026-06-01T05:44:49+09:00", nullable = true)
        private OffsetDateTime beforeStartAt;

        @Schema(description = "변경 전 계획 종료 일시", example = "2026-06-02T13:46:01+09:00", nullable = true)
        private OffsetDateTime beforeEndAt;

        @Schema(description = "변경 전 계획 생산 수량", example = "18700", nullable = true)
        private Integer beforeQuantity;

        @Schema(description = "변경 후 예상 완료일", example = "2026-05-02", nullable = true)
        private LocalDate expectedCompletionDate;

        @Schema(description = "변경 후 납기 지연 여부", example = "false")
        @NotNull(message = "변경 후 납기 지연 여부는 필수입니다.")
        private Boolean afterDelayed;

        @Schema(description = "변경 사유", example = "납기 지연 위험 감소를 위한 라인 재배정", nullable = true)
        private String changeReason;

        public PlanStatus resolvePlanStatus() {
            return planStatus != null ? planStatus : PlanStatus.SCHEDULED;
        }
    }
}