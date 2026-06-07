package s_map.server.domain.order.repository;

import java.math.BigDecimal;

public interface LineCapabilityProjection {

    Integer getCapacityPerDay();

    BigDecimal getStandardProductionTimeHr();
}
