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
        name = "production_lines",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_production_lines_line_code", columnNames = "line_code")
        },
        indexes = {
                @Index(name = "idx_production_lines_is_active", columnList = "is_active")
        }
)
public class ProductionLine extends LastModifiedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "line_id")
    private Long lineId;

    @Column(name = "line_code", nullable = false, unique = true, length = 50)
    private String lineCode;

    @Column(name = "line_name", nullable = false, length = 100)
    private String lineName;

    @Column(name = "max_capacity_per_day", nullable = false)
    private Integer maxCapacityPerDay;

    @Column(name = "capacity_unit", nullable = false, length = 20)
    private String capacityUnit;

    @Column(name = "supports_changeover", nullable = false)
    private boolean supportsChangeover;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "description", columnDefinition = "text")
    private String description;
}
