package s_map.server.domain.report.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportJob;

import java.time.LocalDate;

@Getter
@Builder
public class ReportGenerateResponse {

    private Long reportId;
    private Long reportJobId;
    private String jobStatus;
    private String title;
    private String reportType;
    private LocalDate targetStartDate;
    private LocalDate targetEndDate;
    private JsonNode sections;
    private JsonNode evidence;
    private String markdown;

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
                .markdown(report.getReportContent())
                .build();
    }
}