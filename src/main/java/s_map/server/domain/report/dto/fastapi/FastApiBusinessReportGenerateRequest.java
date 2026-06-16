package s_map.server.domain.report.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportType;
import s_map.server.domain.report.support.ReportPeriodSupport;
import s_map.server.domain.report.support.ReportPeriodSupport.ResolvedPeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "FastAPI 경영진용 보고서 생성 요청")
public class FastApiBusinessReportGenerateRequest {

    @Schema(description = "원본 보고서 고유 ID", example = "12")
    @JsonProperty("report_id")
    private Long reportId;

    @Schema(description = "경영진용 보고서 생성을 위한 원본 보고서 데이터")
    @JsonProperty("source_report")
    private SourceReport sourceReport;

    public static FastApiBusinessReportGenerateRequest from(Report sourceReport) {
        return FastApiBusinessReportGenerateRequest.builder()
                .reportId(sourceReport.getReportId())
                .sourceReport(SourceReport.from(sourceReport))
                .build();
    }

    @Getter
    @Builder
    @Schema(description = "경영진용 보고서 원본 보고서 데이터")
    public static class SourceReport {

        @Schema(description = "원본 보고서 고유 ID", example = "12")
        @JsonProperty("report_id")
        private Long reportId;

        @Schema(description = "원본 보고서 제목", example = "2026년 6월 생산 현황 보고서")
        @JsonProperty("report_title")
        private String reportTitle;

        @Schema(description = "FastAPI에 전달할 보고서 유형", example = "MONTHLY")
        @JsonProperty("report_type")
        private String reportType;

        @Schema(description = "작성자 고유 ID", example = "16")
        @JsonProperty("author_id")
        private Long authorId;

        @Schema(description = "보고서 대상 시작일", example = "2026-06-01", nullable = true)
        @JsonProperty("target_start_date")
        private String targetStartDate;

        @Schema(description = "보고서 대상 종료일", example = "2026-06-30", nullable = true)
        @JsonProperty("target_end_date")
        private String targetEndDate;

        @Schema(description = "원본 보고서 Markdown 본문", nullable = true)
        @JsonProperty("markdown")
        private String markdown;

        @Schema(description = "보고서 포함 항목 JSON")
        @JsonProperty("sections")
        private JsonNode sections;

        @Schema(description = "원본 보고서 상세 내용 JSON")
        @JsonProperty("report_content")
        private JsonNode reportContent;

        @Schema(description = "원본 보고서 근거 데이터 JSON")
        @JsonProperty("report_evidence")
        private JsonNode reportEvidence;

        @Schema(description = "연관 시뮬레이션 고유 ID", example = "7", nullable = true)
        @JsonProperty("related_simulation_id")
        private Long relatedSimulationId;

        @Schema(description = "보고서 생성 일시", example = "2026-06-16T09:00:00", nullable = true)
        @JsonProperty("created_at")
        private String createdAt;

        @Schema(description = "보고서 수정 일시", example = "2026-06-16T10:00:00", nullable = true)
        @JsonProperty("updated_at")
        private String updatedAt;

        private static SourceReport from(Report report) {
            ResolvedPeriod period = ReportPeriodSupport.resolve(
                    report.getReportType(),
                    report.getTargetStartDate(),
                    report.getTargetEndDate()
            );

            return SourceReport.builder()
                    .reportId(report.getReportId())
                    .reportTitle(report.getReportTitle())
                    .reportType(toFastApiReportType(report.getReportType()))
                    .authorId(report.getAuthorId())
                    .targetStartDate(format(period.startDate()))
                    .targetEndDate(format(period.endDate()))
                    .markdown(extractMarkdown(report.getReportContent()))
                    .sections(report.getIncludedItems())
                    .reportContent(report.getReportContent())
                    .reportEvidence(report.getReportEvidence())
                    .relatedSimulationId(report.getRelatedSimulationId())
                    .createdAt(format(report.getCreatedAt()))
                    .updatedAt(format(report.getUpdatedAt()))
                    .build();
        }
    }

    private static String extractMarkdown(JsonNode reportContent) {
        if (reportContent == null || reportContent.isNull()) {
            return null;
        }

        JsonNode markdownNode = reportContent.path("markdown");
        if (markdownNode.isMissingNode() || markdownNode.isNull()) {
            return null;
        }

        return markdownNode.asText();
    }

    private static String format(LocalDate value) {
        return value != null ? value.toString() : null;
    }

    private static String format(LocalDateTime value) {
        return value != null ? value.toString() : null;
    }

    private static String toFastApiReportType(ReportType reportType) {
        if (reportType == null) {
            return null;
        }

        return switch (reportType) {
            case ON_DEMAND, ON_DEMAND_BUSINESS -> "ON_DEMAND";
            case MONTHLY, MONTHLY_BUSINESS -> "MONTHLY";
        };
    }
}
