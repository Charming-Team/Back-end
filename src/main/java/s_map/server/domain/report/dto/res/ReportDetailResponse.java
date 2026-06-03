package s_map.server.domain.report.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.entity.Report;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ReportDetailResponse {

    private Long reportId;
    private String title;
    private String reportType;
    private Long authorId;
    private String authorName;
    private LocalDate targetStartDate;
    private LocalDate targetEndDate;
    private Object sections;
    private Object evidence;
    private String markdown;
    private Long relatedSimulationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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

    public static ReportDetailResponse from(Report report, String authorName) {
        return ReportDetailResponse.builder()
                .reportId(report.getReportId())
                .title(report.getReportTitle())
                .reportType(report.getReportType().name())
                .authorId(report.getAuthorId())
                .authorName(authorName)
                .targetStartDate(report.getTargetStartDate())
                .targetEndDate(report.getTargetEndDate())
                .sections(report.getIncludedItems())
                .evidence(report.getReportEvidence())
                .markdown(extractMarkdown(report.getReportContent()))
                .relatedSimulationId(report.getRelatedSimulationId())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
