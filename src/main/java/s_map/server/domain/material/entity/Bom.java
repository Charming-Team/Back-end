package s_map.server.domain.material.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import s_map.server.global.common.LastModifiedEntity;

import java.math.BigDecimal;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "boms",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_boms_product_material",
                        columnNames = {"product_id", "material_id"}
                )
        }
)
public class Bom extends LastModifiedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bom_id")
    private Long bomId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "required_quantity_per_unit", nullable = false, precision = 12, scale = 4)
    private BigDecimal requiredQuantityPerUnit;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "loss_rate", precision = 5, scale = 2)
    private BigDecimal lossRate;

    public void update(
            BigDecimal requiredQuantityPerUnit,
            String unit,
            BigDecimal lossRate
    ) {
        this.requiredQuantityPerUnit = requiredQuantityPerUnit;
        this.unit = unit;
        this.lossRate = zeroIfNull(lossRate);
    }

    public BigDecimal calculateRequiredQuantity(BigDecimal plannedQuantity) {
        BigDecimal baseQuantity = plannedQuantity.multiply(this.requiredQuantityPerUnit);

        if (this.lossRate == null || this.lossRate.compareTo(BigDecimal.ZERO) == 0) {
            return baseQuantity;
        }

        BigDecimal lossMultiplier = BigDecimal.ONE.add(
                this.lossRate.divide(BigDecimal.valueOf(100))
        );

        return baseQuantity.multiply(lossMultiplier);
    }

    @PrePersist
    public void prePersist() {
        this.lossRate = zeroIfNull(this.lossRate);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
