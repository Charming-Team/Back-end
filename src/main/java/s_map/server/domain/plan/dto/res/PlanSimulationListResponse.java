package s_map.server.domain.plan.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder
public class PlanSimulationListResponse {

    private Long simulationId;
    private String simulationGroupId;
    private String simulationName;

    private Long predictionId;
    private String simulationType;

    private String actionResult;

    private BigDecimal beforeTotalDelayHr;
    private BigDecimal afterTotalDelayHr;
    private BigDecimal delayReductionHr;

    private BigDecimal beforeAvgLineUtilizationRate;
    private BigDecimal afterAvgLineUtilizationRate;

    private Integer beforeTotalProductionQuantity;
    private Integer afterTotalProductionQuantity;

    private Long beforeBottleneckLineId;
    private String beforeBottleneckLineName;

    private Long afterBottleneckLineId;
    private String afterBottleneckLineName;

    private BigDecimal costChangeAmount;
    private String recommendationGrade;

    private Long appliedBy;
    private OffsetDateTime appliedAt;
    private boolean applied;

    private OffsetDateTime createdAt;
}