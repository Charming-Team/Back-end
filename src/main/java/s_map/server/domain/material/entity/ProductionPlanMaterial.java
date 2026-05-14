package s_map.server.domain.material.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(
        name = "production_plan_materials",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_production_plan_materials_plan_material",
                        columnNames = {"plan_id", "material_id"}
                )
        }
)
public class ProductionPlanMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plan_material_id")
    private Long planMaterialId;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "required_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal requiredQuantity;

    @Column(name = "reserved_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal reservedQuantity;

    @Column(name = "consumed_quantity", precision = 12, scale = 4)
    private BigDecimal consumedQuantity;

    @Column(name = "shortage_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal shortageQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "material_plan_status", nullable = false, length = 30)
    private MaterialPlanStatus materialPlanStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateCalculationResult(
            BigDecimal requiredQuantity,
            BigDecimal reservedQuantity,
            BigDecimal consumedQuantity,
            BigDecimal shortageQuantity,
            MaterialPlanStatus materialPlanStatus
    ) {
        this.requiredQuantity = zeroIfNull(requiredQuantity);
        this.reservedQuantity = zeroIfNull(reservedQuantity);
        this.consumedQuantity = zeroIfNull(consumedQuantity);
        this.shortageQuantity = zeroIfNull(shortageQuantity);
        this.materialPlanStatus = materialPlanStatus;
    }

    public boolean hasShortage() {
        return this.shortageQuantity != null
                && this.shortageQuantity.compareTo(BigDecimal.ZERO) > 0;
    }

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.requiredQuantity == null) {
            this.requiredQuantity = BigDecimal.ZERO;
        }

        if (this.reservedQuantity == null) {
            this.reservedQuantity = BigDecimal.ZERO;
        }

        if (this.consumedQuantity == null) {
            this.consumedQuantity = BigDecimal.ZERO;
        }

        if (this.shortageQuantity == null) {
            this.shortageQuantity = BigDecimal.ZERO;
        }

        if (this.materialPlanStatus == null) {
            this.materialPlanStatus = MaterialPlanStatus.READY;
        }
    }

    private BigDecimal zeroIfNull(BigDecimal quantity) {
        return quantity != null ? quantity : BigDecimal.ZERO;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
