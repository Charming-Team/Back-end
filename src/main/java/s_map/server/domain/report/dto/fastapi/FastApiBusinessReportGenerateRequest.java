package s_map.server.domain.report.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
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
public class FastApiBusinessReportGenerateRequest {

    @JsonProperty("report_id")
    private Long reportId;

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
    public static class SourceReport {

        @JsonProperty("report_id")
        private Long reportId;

        @JsonProperty("report_title")
        private String reportTitle;

        @JsonProperty("report_type")
        private String reportType;

        @JsonProperty("author_id")
        private Long authorId;

        @JsonProperty("target_start_date")
        private String targetStartDate;

        @JsonProperty("target_end_date")
        private String targetEndDate;

        @JsonProperty("markdown")
        private String markdown;

        @JsonProperty("sections")
        private JsonNode sections;

        @JsonProperty("report_content")
        private JsonNode reportContent;

        @JsonProperty("report_evidence")
        private JsonNode reportEvidence;

        @JsonProperty("related_simulation_id")
        private Long relatedSimulationId;

        @JsonProperty("created_at")
        private String createdAt;

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
            case ON_DEMAND, ON_DEMAND_BUSINESS -> "AD_HOC";
            case MONTHLY, MONTHLY_BUSINESS -> "MONTHLY";
        };
    }
}
