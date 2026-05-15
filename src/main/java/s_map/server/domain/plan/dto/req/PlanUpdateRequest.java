package s_map.server.domain.plan.dto.req;

import lombok.Getter;
import s_map.server.domain.plan.entity.PlanStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PlanUpdateRequest {

    private Long lineId;
    private Long operatorId;
    private LocalDateTime plannedStartAt;
    private LocalDateTime plannedEndAt;
    private BigDecimal estimatedDurationHr;
    private Integer plannedQuantity;
    private Integer planSequence;
    private PlanStatus planStatus;
}