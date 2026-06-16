package s_map.server.domain.report.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "FastAPI 경영진용 보고서 생성 응답")
public class FastApiBusinessReportGenerateResponse {

    @Schema(description = "생성된 보고서 고유 ID", example = "21")
    @JsonProperty("report_id")
    private Long reportId;

    @Schema(description = "보고서 유형", example = "MONTHLY")
    @JsonProperty("report_type")
    private String reportType;

    @Schema(description = "보고서 제목", example = "2026년 6월 경영진용 생산 현황 보고서")
    @JsonProperty("report_title")
    private String reportTitle;

    @Schema(description = "작성자 고유 ID", example = "16")
    @JsonProperty("author_id")
    private Long authorId;

    @Schema(description = "보고서 대상 시작일", example = "2026-06-01", nullable = true)
    @JsonProperty("target_start_date")
    private String targetStartDate;

    @Schema(description = "보고서 대상 종료일", example = "2026-06-30", nullable = true)
    @JsonProperty("target_end_date")
    private String targetEndDate;

    @Schema(description = "생성된 보고서 상세 내용")
    @JsonProperty("report_content")
    private Object reportContent;

    @Schema(description = "보고서 근거 데이터")
    @JsonProperty("report_evidence")
    private Object reportEvidence;

    @Schema(description = "연관 시뮬레이션 고유 ID", example = "7", nullable = true)
    @JsonProperty("related_simulation_id")
    private Long relatedSimulationId;

    @Schema(description = "보고서 생성 일시", example = "2026-06-16T09:00:00", nullable = true)
    @JsonProperty("created_at")
    private String createdAt;

    @Schema(description = "보고서 수정 일시", example = "2026-06-16T10:00:00", nullable = true)
    @JsonProperty("updated_at")
    private String updatedAt;
}
