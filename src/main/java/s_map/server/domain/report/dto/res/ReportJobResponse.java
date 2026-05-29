package s_map.server.domain.report.dto.res;

import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.entity.ReportJob;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReportJobResponse {

    private Long reportJobId;
    private Long reportId;
    private Long requestedBy;
    private String jobStatus;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReportJobResponse from(ReportJob reportJob) {
        return ReportJobResponse.builder()
                .reportJobId(reportJob.getJobId())
                .reportId(reportJob.getReportId())
                .requestedBy(reportJob.getRequestedBy())
                .jobStatus(reportJob.getJobStatus().name())
                .errorMessage(reportJob.getErrorMessage())
                .retryCount(reportJob.getRetryCount())
                .startedAt(reportJob.getStartedAt())
                .finishedAt(reportJob.getFinishedAt())
                .createdAt(reportJob.getCreatedAt())
                .updatedAt(reportJob.getUpdatedAt())
                .build();
    }
}