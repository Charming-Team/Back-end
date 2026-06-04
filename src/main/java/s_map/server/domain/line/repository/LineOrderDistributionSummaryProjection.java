package s_map.server.domain.line.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface LineOrderDistributionSummaryProjection {

    Long getOrderId();
    String getOrderNo();
    Long getProductId();
    String getProductName();
    String getProductUnit();
    Integer getOrderQuantity();
    LocalDate getDueDate();
    Integer getAssignedLineCount();
    BigDecimal getTotalPlannedQuantity();
    BigDecimal getTotalProductionQuantity();
}
