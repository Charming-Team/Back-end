package s_map.server.domain.plan.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
public class PlanSimulationDetailResponse {

    private Long simulationDetailId;
    private Long simulationId;

    private Long planId;
    private Long orderId;

    private Long beforeLineId;
    private String beforeLineName;

    private Long afterLineId;
    private String afterLineName;

    private Integer beforeSequence;
    private Integer afterSequence;

    private OffsetDateTime beforeStartAt;
    private OffsetDateTime beforeEndAt;

    private OffsetDateTime afterStartAt;
    private OffsetDateTime afterEndAt;

    private LocalDate expectedCompletionDate;
    private Boolean afterDelayed;

    private Integer beforeQuantity;
    private Integer afterQuantity;

    private String changeReason;

    private OffsetDateTime createdAt;
}