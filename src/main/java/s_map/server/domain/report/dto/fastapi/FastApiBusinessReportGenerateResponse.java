package s_map.server.domain.report.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class FastApiBusinessReportGenerateResponse {

    @JsonProperty("report_id")
    @JsonAlias("reportId")
    private Long reportId;

    @JsonProperty("report_type")
    @JsonAlias("reportType")
    private String reportType;

    @JsonProperty("report_title")
    @JsonAlias("reportTitle")
    private String reportTitle;

    @JsonProperty("author_id")
    @JsonAlias("authorId")
    private Long authorId;

    @JsonProperty("target_start_date")
    @JsonAlias("targetStartDate")
    private String targetStartDate;

    @JsonProperty("target_end_date")
    @JsonAlias("targetEndDate")
    private String targetEndDate;

    @JsonProperty("report_content")
    @JsonAlias("reportContent")
    private Object reportContent;

    @JsonProperty("report_evidence")
    @JsonAlias("reportEvidence")
    private Object reportEvidence;

    @JsonProperty("related_simulation_id")
    @JsonAlias("relatedSimulationId")
    private Long relatedSimulationId;

    @JsonProperty("created_at")
    @JsonAlias("createdAt")
    private String createdAt;

    @JsonProperty("updated_at")
    @JsonAlias("updatedAt")
    private String updatedAt;
}
