package s_map.server.domain.order.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface LineAssignmentCandidateProjection {
    Long getLineId();
    String getLineName();
    Integer getCapacityPerDay();
    BigDecimal getStandardProductionTimeHr();
    Integer getPriorityRank();
    OffsetDateTime getLastPlannedEndAt();
    Integer getLastPlanSequence();
}