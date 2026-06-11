package s_map.server.domain.report.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "생성 완료된 보고서 엔티티")
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    @Schema(description = "보고서 ID", example = "1")
    private Long reportId;

    @Column(name = "report_title", nullable = false, length = 200)
    @Schema(description = "보고서 제목", example = "2026-05-01 ~ 2026-05-31 월간 보고서")
    private String reportTitle;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "report_type", nullable = false, columnDefinition = "report_type_enum")
    @Schema(description = "보고서 유형", example = "MONTHLY")
    private ReportType reportType;

    @Column(name = "author_id", nullable = false)
    @Schema(description = "보고서 작성자 사용자 ID", example = "1")
    private Long authorId;

    @Column(name = "target_start_date", nullable = false)
    @Schema(description = "보고서 대상 시작일", example = "2026-05-01")
    private LocalDate targetStartDate;

    @Column(name = "target_end_date", nullable = false)
    @Schema(description = "보고서 대상 종료일", example = "2026-05-31")
    private LocalDate targetEndDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "included_items", columnDefinition = "jsonb")
    @Schema(description = "보고서 화면 섹션 데이터 JSON")
    private JsonNode includedItems;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_content", nullable = false, columnDefinition = "jsonb")
    @Schema(description = "보고서 본문 JSON. markdown 필드에 Markdown 본문을 저장합니다.")
    private JsonNode reportContent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_evidence", columnDefinition = "jsonb")
    @Schema(description = "보고서 생성 근거 데이터 JSON")
    private JsonNode reportEvidence;

    @Column(name = "related_simulation_id")
    @Schema(description = "보고서와 연결된 시뮬레이션 ID", example = "1001", nullable = true)
    private Long relatedSimulationId;

    public void updateIncludedItems(JsonNode includedItems) {
        this.includedItems = includedItems;
    }

    public void updateReport(String reportTitle, JsonNode reportContent, JsonNode includedItems) {
        this.reportTitle = reportTitle;
        this.reportContent = reportContent;
        this.includedItems = includedItems;
    }
}
