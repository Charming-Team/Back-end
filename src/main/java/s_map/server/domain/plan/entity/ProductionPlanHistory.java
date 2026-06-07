package s_map.server.domain.plan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.global.common.BaseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(
        name = "production_plan_histories",
        indexes = {
                @Index(name = "idx_production_plan_histories_snapshot", columnList = "rollback_snapshot_id"),
                @Index(name = "idx_production_plan_histories_source_plan", columnList = "source_plan_id"),
                @Index(name = "idx_production_plan_histories_apply_history", columnList = "apply_history_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductionPlanHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_history_id")
    private Long planHistoryId;

    @Column(name = "apply_history_id", nullable = false)
    private Long applyHistoryId;

    @Column(name = "rollback_snapshot_id", nullable = false, length = 36)
    private String rollbackSnapshotId;

    @Column(name = "source_plan_id", nullable = false)
    private Long sourcePlanId;

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

    @Column(name = "plan_status", nullable = false, length = 30)
    private String planStatus;

    @Column(name = "source_is_current", nullable = false)
    private boolean sourceCurrent;

    @Column(name = "source_created_at", nullable = false)
    private LocalDateTime sourceCreatedAt;

    @Column(name = "source_updated_at", nullable = false)
    private LocalDateTime sourceUpdatedAt;

    @Builder
    private ProductionPlanHistory(
            Long applyHistoryId,
            String rollbackSnapshotId,
            Long sourcePlanId,
            Long orderId,
            Long productId,
            Long lineId,
            Long operatorId,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            BigDecimal estimatedDurationHr,
            Integer plannedQuantity,
            Integer planSequence,
            String planStatus,
            boolean sourceCurrent,
            LocalDateTime sourceCreatedAt,
            LocalDateTime sourceUpdatedAt
    ) {
        this.applyHistoryId = applyHistoryId;
        this.rollbackSnapshotId = rollbackSnapshotId;
        this.sourcePlanId = sourcePlanId;
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
        this.sourceCurrent = sourceCurrent;
        this.sourceCreatedAt = sourceCreatedAt;
        this.sourceUpdatedAt = sourceUpdatedAt;
    }

    public static ProductionPlanHistory snapshotOf(
            Long applyHistoryId,
            String rollbackSnapshotId,
            ProductionPlan plan
    ) {
        return ProductionPlanHistory.builder()
                .applyHistoryId(applyHistoryId)
                .rollbackSnapshotId(rollbackSnapshotId)
                .sourcePlanId(plan.getPlanId())
                .orderId(plan.getOrderId())
                .productId(plan.getProductId())
                .lineId(plan.getLineId())
                .operatorId(plan.getOperatorId())
                .plannedStartAt(plan.getPlannedStartAt())
                .plannedEndAt(plan.getPlannedEndAt())
                .estimatedDurationHr(plan.getEstimatedDurationHr())
                .plannedQuantity(plan.getPlannedQuantity())
                .planSequence(plan.getPlanSequence())
                .planStatus(plan.getPlanStatus().name())
                .sourceCurrent(plan.isCurrent())
                .sourceCreatedAt(plan.getCreatedAt())
                .sourceUpdatedAt(plan.getUpdatedAt())
                .build();
    }
}
