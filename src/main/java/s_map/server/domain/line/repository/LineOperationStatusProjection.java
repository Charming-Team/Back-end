package s_map.server.domain.line.repository;

import java.math.BigDecimal;
import java.time.Instant;

public interface LineOperationStatusProjection {

    Long getLineId();
    String getLineCode();
    String getLineName();
    BigDecimal getUtilizationRate();
    Long getCurrentProductId();
    String getCurrentProductName();
    Long getNextProductId();
    String getNextProductName();
    String getOperationStatus();
    Instant getTransitionAt();
    Instant getRecordedAt();
}
