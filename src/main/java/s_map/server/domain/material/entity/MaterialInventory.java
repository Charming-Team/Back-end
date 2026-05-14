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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "material_inventories")
public class MaterialInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inventory_id")
    private Long inventoryId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false, unique = true)
    private Material material;

    @Column(name = "current_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal currentQuantity;

    @Column(name = "available_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal availableQuantity;

    @Column(name = "reserved_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal reservedQuantity;

    @Column(name = "safety_stock_quantity", nullable = false, precision = 12, scale = 4)
    private BigDecimal safetyStockQuantity;

    @Column(name = "expected_inbound_at")
    private LocalDateTime expectedInboundAt;

    @Column(name = "expected_inbound_quantity", precision = 12, scale = 4)
    private BigDecimal expectedInboundQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "inventory_status", nullable = false, length = 30)
    private InventoryStatus inventoryStatus;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void updateInventory(
            BigDecimal currentQuantity,
            BigDecimal reservedQuantity,
            BigDecimal safetyStockQuantity,
            LocalDateTime expectedInboundAt,
            BigDecimal expectedInboundQuantity
    ) {
        this.currentQuantity = currentQuantity;
        this.reservedQuantity = reservedQuantity;
        this.safetyStockQuantity = safetyStockQuantity;
        this.expectedInboundAt = expectedInboundAt;
        this.expectedInboundQuantity = expectedInboundQuantity;
        recalculateInventoryState();
    }

    public void reserve(BigDecimal quantity) {
        validatePositiveQuantity(quantity);

        if (this.availableQuantity.compareTo(quantity) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_AVAILABLE_INVENTORY);
        }

        this.reservedQuantity = this.reservedQuantity.add(quantity);
        this.availableQuantity = this.currentQuantity.subtract(this.reservedQuantity);
        refreshInventoryStatus();
    }

    public void releaseReserved(BigDecimal quantity) {
        validatePositiveQuantity(quantity);

        if (this.reservedQuantity.compareTo(quantity) < 0) {
            throw new CustomException(ErrorCode.INVALID_INVENTORY_RELEASE_QUANTITY);
        }

        this.reservedQuantity = this.reservedQuantity.subtract(quantity);
        this.availableQuantity = this.currentQuantity.subtract(this.reservedQuantity);
        refreshInventoryStatus();
    }

    private void validatePositiveQuantity(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(ErrorCode.INVALID_INVENTORY_OPERATION_QUANTITY);
        }
    }

    private void recalculateInventoryState() {
        validateInventoryQuantities();
        this.availableQuantity = this.currentQuantity.subtract(this.reservedQuantity);
        refreshInventoryStatus();
    }

    private void validateInventoryQuantities() {
        if (this.currentQuantity == null
                || this.reservedQuantity == null
                || this.safetyStockQuantity == null
                || this.currentQuantity.compareTo(BigDecimal.ZERO) < 0
                || this.reservedQuantity.compareTo(BigDecimal.ZERO) < 0
                || this.safetyStockQuantity.compareTo(BigDecimal.ZERO) < 0
                || (this.expectedInboundQuantity != null
                && this.expectedInboundQuantity.compareTo(BigDecimal.ZERO) < 0)
                || this.reservedQuantity.compareTo(this.currentQuantity) > 0) {
            throw new CustomException(ErrorCode.INVALID_INVENTORY_QUANTITY);
        }
    }

    public void refreshInventoryStatus() {
        if (this.availableQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            this.inventoryStatus = InventoryStatus.SHORTAGE;
            return;
        }

        if (this.availableQuantity.compareTo(this.safetyStockQuantity) < 0) {
            this.inventoryStatus = InventoryStatus.LOW;
            return;
        }

        if (this.expectedInboundAt != null && this.expectedInboundQuantity != null
                && this.expectedInboundQuantity.compareTo(BigDecimal.ZERO) > 0) {
            this.inventoryStatus = InventoryStatus.INBOUND_WAITING;
            return;
        }

        this.inventoryStatus = InventoryStatus.NORMAL;
    }

    @PrePersist
    public void prePersist() {
        this.updatedAt = LocalDateTime.now();

        if (this.currentQuantity == null) {
            this.currentQuantity = BigDecimal.ZERO;
        }

        if (this.reservedQuantity == null) {
            this.reservedQuantity = BigDecimal.ZERO;
        }

        if (this.availableQuantity == null) {
            this.availableQuantity = this.currentQuantity.subtract(this.reservedQuantity);
        }

        if (this.safetyStockQuantity == null) {
            this.safetyStockQuantity = BigDecimal.ZERO;
        }

        recalculateInventoryState();
    }

    @PreUpdate
    public void preUpdate() {
        recalculateInventoryState();
        this.updatedAt = LocalDateTime.now();
    }
}
