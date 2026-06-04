package s_map.server.domain.line.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "line_status",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_line_status_line_recorded_at",
                        columnNames = {"line_id", "recorded_at"}
                )
        },
        indexes = {
                @Index(name = "idx_line_status_line_recorded_at", columnList = "line_id, recorded_at"),
                @Index(name = "idx_line_status_plan_id", columnList = "plan_id")
        }
)
public class LineStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_status_id")
    private Long lineStatusId;

    @Column(name = "line_id", nullable = false)
    private Long lineId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::operation_status_enum")
    @Column(name = "operation_status", nullable = false, columnDefinition = "operation_status_enum")
    private OperationStatus operationStatus;

    @Column(name = "throughput_rate", precision = 5, scale = 4)
    private BigDecimal throughputRate;

    @Column(name = "current_yield_rate", precision = 5, scale = 4)
    private BigDecimal currentYieldRate;

    @Column(name = "waiting_quantity")
    private Integer waitingQuantity;

    @Column(name = "waiting_time_hr", precision = 10, scale = 2)
    private BigDecimal waitingTimeHr;

    @Column(name = "processed_quantity")
    private Integer processedQuantity;

    @Column(name = "defect_quantity")
    private Integer defectQuantity;

    @Column(name = "utilization_rate", precision = 5, scale = 4)
    private BigDecimal utilizationRate;

    @Column(name = "progress_rate", precision = 5, scale = 4)
    private BigDecimal progressRate;
}
