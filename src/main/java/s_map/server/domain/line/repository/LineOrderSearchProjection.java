package s_map.server.domain.line.repository;

import java.time.LocalDate;

public interface LineOrderSearchProjection {

    Long getOrderId();
    String getOrderNo();
    Long getProductId();
    String getProductName();
    Integer getOrderQuantity();
    LocalDate getDueDate();
    String getLineNames();
}
