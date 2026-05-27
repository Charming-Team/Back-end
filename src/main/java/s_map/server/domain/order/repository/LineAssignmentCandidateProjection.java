package s_map.server.domain.order.repository;

import java.math.BigDecimal;
import java.time.Instant;

public interface LineAssignmentCandidateProjection {
    Long getLineId();
    String getLineName();
    Integer getCapacityPerDay();
    BigDecimal getStandardProductionTimeHr();
    Integer getPriorityRank();
    Instant getLastPlannedEndAt();
    Integer getLastPlanSequence();
}
