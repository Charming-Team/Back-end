package s_map.server.domain.report.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class FastApiBusinessReportGenerateResponse {

    @JsonProperty("report_id")
    private Long reportId;

    @JsonProperty("report_type")
    private String reportType;

    @JsonProperty("report_title")
    private String reportTitle;

    @JsonProperty("author_id")
    private Long authorId;

    @JsonProperty("target_start_date")
    private String targetStartDate;

    @JsonProperty("target_end_date")
    private String targetEndDate;

    @JsonProperty("report_content")
    private Object reportContent;

    @JsonProperty("report_evidence")
    private Object reportEvidence;

    @JsonProperty("related_simulation_id")
    private Long relatedSimulationId;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;
}