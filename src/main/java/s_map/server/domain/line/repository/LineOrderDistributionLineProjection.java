package s_map.server.domain.line.repository;

import java.math.BigDecimal;
import java.time.Instant;

public interface LineOrderDistributionLineProjection {

    Long getLineId();
    String getLineCode();
    String getLineName();
    Long getProductId();
    String getProductName();
    String getProductUnit();
    BigDecimal getPlannedQuantity();
    BigDecimal getProductionQuantity();
    String getOperationStatus();
    Instant getTransitionAt();
    Instant getRecordedAt();
}
