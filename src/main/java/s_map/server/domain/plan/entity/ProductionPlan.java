package s_map.server.domain.plan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "production_plans")
public class ProductionPlan {

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
    private LocalDateTime plannedStartAt;

    @Column(name = "planned_end_at", nullable = false)
    private LocalDateTime plannedEndAt;

    @Column(name = "estimated_duration_hr", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedDurationHr;

    @Column(name = "planned_quantity", nullable = false)
    private Integer plannedQuantity;

    @Column(name = "plan_sequence", nullable = false)
    private Integer planSequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_status", nullable = false, length = 30)
    private PlanStatus planStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updatePlan(
            Long lineId,
            Long operatorId,
            LocalDateTime plannedStartAt,
            LocalDateTime plannedEndAt,
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

    public boolean isCurrentTarget() {
        return this.planStatus == PlanStatus.SCHEDULED
                || this.planStatus == PlanStatus.IN_PROGRESS
                || this.planStatus == PlanStatus.DELAYED;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.planStatus == null) {
            this.planStatus = PlanStatus.SCHEDULED;
        }

        if (this.planSequence == null) {
            this.planSequence = 0;
        }

        if (this.estimatedDurationHr == null) {
            this.estimatedDurationHr = BigDecimal.ZERO;
        }

        if (this.plannedQuantity == null) {
            this.plannedQuantity = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}