package s_map.server.domain.report.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportJob;

import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "보고서 생성 완료 응답")
public class ReportGenerateResponse {

    @Schema(description = "생성된 보고서 ID", example = "1")
    private Long reportId;

    @Schema(description = "보고서 생성 Job ID", example = "10")
    private Long reportJobId;

    @Schema(description = "보고서 생성 Job 상태", example = "SUCCESS")
    private String jobStatus;

    @Schema(description = "보고서 제목", example = "2026년 5월 월간 생산 보고서")
    private String title;

    @Schema(description = "보고서 유형", example = "MONTHLY")
    private String reportType;

    @Schema(description = "보고서 대상 시작일", example = "2026-05-01")
    private LocalDate targetStartDate;

    @Schema(description = "보고서 대상 종료일", example = "2026-05-31")
    private LocalDate targetEndDate;

    @Schema(description = "보고서 상세 화면 섹션 데이터")
    private Object sections;

    @Schema(description = "보고서 생성 근거 데이터")
    private Object evidence;

    @Schema(description = "보고서 본문 Markdown", example = "## 주요 요약\\n- 총 생산량이 계획 대비 98%를 달성했습니다.")
    private String markdown;

    private static String extractMarkdown(JsonNode reportContent) {
        if (reportContent == null || reportContent.isNull()) {
            return null;
        }

        JsonNode markdownNode = reportContent.path("markdown");

        if (markdownNode.isMissingNode() || markdownNode.isNull()) {
            return reportContent.toString();
        }

        return markdownNode.asText();
    }

    public static ReportGenerateResponse of(Report report, ReportJob reportJob) {
        return ReportGenerateResponse.builder()
                .reportId(report.getReportId())
                .reportJobId(reportJob.getJobId())
                .jobStatus(reportJob.getJobStatus().name())
                .title(report.getReportTitle())
                .reportType(report.getReportType().name())
                .targetStartDate(report.getTargetStartDate())
                .targetEndDate(report.getTargetEndDate())
                .sections(report.getIncludedItems())
                .evidence(report.getReportEvidence())
                .markdown(extractMarkdown(report.getReportContent()))
                .build();
    }
}
