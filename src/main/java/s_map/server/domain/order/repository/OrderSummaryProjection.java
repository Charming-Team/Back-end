package s_map.server.domain.order.repository;

import java.time.LocalDate;

public interface OrderSummaryProjection {
    Long getOrderId();
    String getOrderNo();
    String getCustomerName();

    Long getProductId();
    String getProductCode();
    String getProductName();

    Integer getOrderQuantity();
    LocalDate getDueDate();
    String getOrderStatus();
}