package s_map.server.domain.report.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import s_map.server.global.common.BaseEntity;

import java.time.LocalDate;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reports")
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "report_title", nullable = false, length = 200)
    private String reportTitle;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "report_type", nullable = false, columnDefinition = "report_type_enum")
    private ReportType reportType;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "target_start_date", nullable = false)
    private LocalDate targetStartDate;

    @Column(name = "target_end_date", nullable = false)
    private LocalDate targetEndDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "included_items", columnDefinition = "jsonb")
    private JsonNode includedItems;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_content", nullable = false, columnDefinition = "jsonb")
    private JsonNode reportContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_evidence", columnDefinition = "jsonb")
    private JsonNode reportEvidence;

    @Column(name = "related_simulation_id")
    private Long relatedSimulationId;
}