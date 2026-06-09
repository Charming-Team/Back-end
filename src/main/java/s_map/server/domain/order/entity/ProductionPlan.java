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
import org.hibernate.annotations.ColumnTransformer;
import s_map.server.global.common.BaseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(
        name = "production_plans",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_production_plans_line_sequence",
                        columnNames = {"line_id", "plan_sequence"}
                )
        },
        indexes = {
                @Index(name = "idx_production_plans_line_status_end", columnList = "line_id, plan_status, planned_end_at"),
                @Index(name = "idx_production_plans_order_id", columnList = "order_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductionPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "line_id", nullable = false)
    private Long lineId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "planned_start_at", nullable = false)
    private OffsetDateTime plannedStartAt;

    @Column(name = "planned_end_at", nullable = false)
    private OffsetDateTime plannedEndAt;

    @Column(name = "estimated_duration_hr", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedDurationHr;

    @Column(name = "planned_quantity", nullable = false)
    private Integer plannedQuantity;

    @Column(name = "plan_sequence", nullable = false)
    private Integer planSequence;

    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::plan_status_enum")
    @Column(name = "plan_status", nullable = false, columnDefinition = "plan_status_enum")
    private PlanStatus planStatus;

    @Builder
    private ProductionPlan(
            Long orderId,
            Long productId,
            Long lineId,
            Long operatorId,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            BigDecimal estimatedDurationHr,
            Integer plannedQuantity,
            Integer planSequence,
            PlanStatus planStatus
    ) {
        this.orderId = orderId;
        this.productId = productId;
        this.lineId = lineId;
        this.operatorId = operatorId;
        this.plannedStartAt = plannedStartAt;
        this.plannedEndAt = plannedEndAt;
        this.estimatedDurationHr = estimatedDurationHr;
        this.plannedQuantity = plannedQuantity;
        this.planSequence = planSequence;
        this.planStatus = planStatus;
    }

    public static ProductionPlan create(
            Long orderId,
            Long productId,
            Long lineId,
            Long operatorId,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            BigDecimal estimatedDurationHr,
            Integer plannedQuantity,
            Integer planSequence
    ) {
        return ProductionPlan.builder()
                .orderId(orderId)
                .productId(productId)
                .lineId(lineId)
                .operatorId(operatorId)
                .plannedStartAt(plannedStartAt)
                .plannedEndAt(plannedEndAt)
                .estimatedDurationHr(estimatedDurationHr)
                .plannedQuantity(plannedQuantity)
                .planSequence(planSequence)
                .planStatus(PlanStatus.SCHEDULED)
                .build();
    }

    public void updatePlan(
            Long lineId,
            Long operatorId,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            Integer plannedQuantity,
            Integer planSequence,
            PlanStatus planStatus
    ) {
        this.lineId = lineId;
        this.operatorId = operatorId;
        this.plannedStartAt = plannedStartAt;
        this.plannedEndAt = plannedEndAt;
        this.plannedQuantity = plannedQuantity;
        this.planSequence = planSequence;
        this.planStatus = planStatus;
    }

    public void applySelectedSimulationPlan(
            Long lineId,
            Long operatorId,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            BigDecimal estimatedDurationHr,
            Integer plannedQuantity,
            Integer planSequence,
            PlanStatus planStatus
    ) {
        this.lineId = lineId;
        this.operatorId = operatorId;
        this.plannedStartAt = plannedStartAt;
        this.plannedEndAt = plannedEndAt;
        this.estimatedDurationHr = estimatedDurationHr;
        this.plannedQuantity = plannedQuantity;
        this.planSequence = planSequence;
        this.planStatus = planStatus;
    }

    public void moveSchedule(
            Long lineId,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt
    ) {
        this.lineId = lineId;
        this.plannedStartAt = plannedStartAt;
        this.plannedEndAt = plannedEndAt;
    }

    public void cancel() {
        this.planStatus = PlanStatus.CANCELLED;
    }
}
