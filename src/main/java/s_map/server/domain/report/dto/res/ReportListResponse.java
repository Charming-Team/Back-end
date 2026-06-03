package s_map.server.domain.report.dto.res;

import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.entity.Report;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class ReportListResponse {

    private Long reportId;
    private String title;
    private String reportType;
    private Long authorId;
    private String authorName;
    private LocalDate targetStartDate;
    private LocalDate targetEndDate;
    private LocalDateTime createdAt;

    public static ReportListResponse from(Report report, String authorName) {
        return ReportListResponse.builder()
                .reportId(report.getReportId())
                .title(report.getReportTitle())
                .reportType(report.getReportType().name())
                .authorId(report.getAuthorId())
                .authorName(authorName)
                .targetStartDate(report.getTargetStartDate())
                .targetEndDate(report.getTargetEndDate())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
