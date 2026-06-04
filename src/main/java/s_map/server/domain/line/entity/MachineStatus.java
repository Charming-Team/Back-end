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
import s_map.server.global.common.LastModifiedEntity;

import java.time.OffsetDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "machine_statuses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_machine_statuses_machine_recorded_at",
                        columnNames = {"machine_id", "recorded_at"}
                )
        },
        indexes = {
                @Index(name = "idx_machine_statuses_line_recorded_at", columnList = "line_id, recorded_at"),
                @Index(name = "idx_machine_statuses_plan_id", columnList = "plan_id")
        }
)
public class MachineStatus extends LastModifiedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "machine_status_id")
    private Long machineStatusId;

    @Column(name = "machine_id", nullable = false)
    private Long machineId;

    @Column(name = "line_id", nullable = false)
    private Long lineId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::operation_status_enum")
    @Column(name = "operation_status", nullable = false, columnDefinition = "operation_status_enum")
    private OperationStatus operationStatus;

    @Column(name = "processed_quantity")
    private Integer processedQuantity;

    @Column(name = "defect_quantity")
    private Integer defectQuantity;

    @Column(name = "status_note", columnDefinition = "text")
    private String statusNote;
}
