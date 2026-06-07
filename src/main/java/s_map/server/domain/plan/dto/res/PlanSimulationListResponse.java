package s_map.server.domain.plan.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "생산계획 시뮬레이션 결과 목록 응답")
@Getter
@Builder
public class PlanSimulationListResponse {

    @Schema(description = "시뮬레이션 결과 ID", example = "1")
    private Long simulationId;
    @Schema(description = "동일 실행 묶음을 식별하는 시뮬레이션 그룹 ID", example = "SIM-GRP-20260605-001")
    private String simulationGroupId;
    @Schema(description = "시뮬레이션명", example = "납기 지연 최소화 시뮬레이션")
    private String simulationName;

    @Schema(description = "연결된 예측 결과 ID", example = "15", nullable = true)
    private Long predictionId;
    @Schema(description = "시뮬레이션 유형", example = "DUE_DATE_OPTIMIZATION")
    private String simulationType;

    @Schema(description = "시뮬레이션 실행 결과 요약", example = "지연 시간이 12.50시간 감소했습니다.", nullable = true)
    private String actionResult;

    @Schema(description = "변경 전 총 지연 시간", example = "36.50")
    private BigDecimal beforeTotalDelayHr;
    @Schema(description = "변경 후 총 지연 시간", example = "24.00")
    private BigDecimal afterTotalDelayHr;
    @Schema(description = "지연 감소 시간", example = "12.50")
    private BigDecimal delayReductionHr;

    @Schema(description = "변경 전 평균 라인 가동률", example = "0.7200", nullable = true)
    private BigDecimal beforeAvgLineUtilizationRate;
    @Schema(description = "변경 후 평균 라인 가동률", example = "0.8150", nullable = true)
    private BigDecimal afterAvgLineUtilizationRate;

    @Schema(description = "변경 전 총 생산 수량", example = "128000", nullable = true)
    private Integer beforeTotalProductionQuantity;
    @Schema(description = "변경 후 총 생산 수량", example = "132000", nullable = true)
    private Integer afterTotalProductionQuantity;

    @Schema(description = "변경 전 병목 라인 ID", example = "2", nullable = true)
    private Long beforeBottleneckLineId;
    @Schema(description = "변경 전 병목 라인명", example = "ABS 보조 생산 Line", nullable = true)
    private String beforeBottleneckLineName;

    @Schema(description = "변경 후 병목 라인 ID", example = "4", nullable = true)
    private Long afterBottleneckLineId;
    @Schema(description = "변경 후 병목 라인명", example = "PP 기능성 생산 Line", nullable = true)
    private String afterBottleneckLineName;

    @Schema(description = "비용 변화 금액. 증가면 양수, 감소면 음수입니다.", example = "-150000.00", nullable = true)
    private BigDecimal costChangeAmount;
    @Schema(description = "추천 등급", example = "A", nullable = true)
    private String recommendationGrade;

    @Schema(description = "시뮬레이션 적용 사용자 ID", example = "7", nullable = true)
    private Long appliedBy;
    @Schema(description = "시뮬레이션 적용 일시. 적용 전이면 null입니다.", example = "2026-06-05T14:20:00+09:00", nullable = true)
    private OffsetDateTime appliedAt;
    @Schema(description = "시뮬레이션 적용 여부", example = "true")
    private boolean applied;

    @Schema(description = "시뮬레이션 결과 생성 일시", example = "2026-06-05T13:30:00+09:00")
    private OffsetDateTime createdAt;
}
