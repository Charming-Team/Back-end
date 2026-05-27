package s_map.server.domain.order.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

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

    Integer getPriorityRank();
    Integer getPlanSequence();
    OffsetDateTime getPlannedStartAt();
    OffsetDateTime getPlannedEndAt();
    BigDecimal getEstimatedDurationHr();

    String getLineNames();
    String getOperatorNames();

    LocalDateTime getCreatedAt();
    LocalDateTime getUpdatedAt();
}
