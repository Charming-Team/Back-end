package s_map.server.domain.order.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public interface OrderDetailProjection {
    Long getOrderId();
    String getOrderNo();

    Long getProductId();
    String getProductCode();
    String getProductName();
    String getProductCategory();
    String getProductUnit();

    Integer getOrderQuantity();
    String getCustomerName();
    String getCustomerContactName();

    LocalDate getOrderDate();
    LocalDate getDueDate();

    BigDecimal getContractAmount();
    BigDecimal getLatePenaltyAmount();

    String getOrderStatus();

    Integer getPlanSequence();
    Instant getPlannedStartAt();
    Instant getPlannedEndAt();
    BigDecimal getEstimatedDurationHr();

    String getLineNames();
    String getOperatorNames();

    Instant getCreatedAt();
    Instant getUpdatedAt();
}