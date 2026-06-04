package s_map.server.domain.line.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import s_map.server.global.common.LastModifiedEntity;

@Getter
@Entity
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "production_machines",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_production_machines_machine_code", columnNames = "machine_code")
        },
        indexes = {
                @Index(name = "idx_production_machines_line_order", columnList = "line_id, machine_order")
        }
)
public class ProductionMachine extends LastModifiedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "machine_id")
    private Long machineId;

    @Column(name = "line_id", nullable = false)
    private Long lineId;

    @Column(name = "machine_code", nullable = false, unique = true, length = 50)
    private String machineCode;

    @Column(name = "machine_name", nullable = false, length = 100)
    private String machineName;

    @Column(name = "machine_type", length = 50)
    private String machineType;

    @Column(name = "machine_role", length = 100)
    private String machineRole;

    @Column(name = "machine_order")
    private Integer machineOrder;
}
