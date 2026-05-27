package s_map.server.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import s_map.server.global.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Entity
@Table(
        name = "customer_orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_customer_orders_order_no", columnNames = "order_no")
        },
        indexes = {
                @Index(name = "idx_customer_orders_due_status", columnList = "due_date, order_status"),
                @Index(name = "idx_customer_orders_product_id", columnList = "product_id"),
                @Index(name = "idx_customer_orders_customer_name", columnList = "customer_name")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CustomerOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_no", nullable = false, length = 50)
    private String orderNo;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "order_quantity", nullable = false)
    private Integer orderQuantity;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_contact_name", length = 50)
    private String customerContactName;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "contract_amount", precision = 15, scale = 2)
    private BigDecimal contractAmount;

    @Column(name = "late_penalty_amount", precision = 15, scale = 2)
    private BigDecimal latePenaltyAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Builder
    private CustomerOrder(
            String orderNo,
            Long productId,
            Integer orderQuantity,
            String customerName,
            String customerContactName,
            LocalDate orderDate,
            LocalDate dueDate,
            BigDecimal contractAmount,
            BigDecimal latePenaltyAmount,
            OrderStatus orderStatus
    ) {
        this.orderNo = orderNo;
        this.productId = productId;
        this.orderQuantity = orderQuantity;
        this.customerName = customerName;
        this.customerContactName = customerContactName;
        this.orderDate = orderDate;
        this.dueDate = dueDate;
        this.contractAmount = contractAmount;
        this.latePenaltyAmount = latePenaltyAmount;
        this.orderStatus = orderStatus;
    }

    public static CustomerOrder create(
            String orderNo,
            Long productId,
            Integer orderQuantity,
            String customerName,
            String customerContactName,
            LocalDate dueDate,
            BigDecimal contractAmount,
            BigDecimal latePenaltyAmount
    ) {
        return CustomerOrder.builder()
                .orderNo(orderNo)
                .productId(productId)
                .orderQuantity(orderQuantity)
                .customerName(customerName)
                .customerContactName(customerContactName)
                .orderDate(LocalDate.now())
                .dueDate(dueDate)
                .contractAmount(contractAmount)
                .latePenaltyAmount(latePenaltyAmount)
                .orderStatus(OrderStatus.WAITING)
                .build();
    }
}
