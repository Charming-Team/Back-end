package s_map.server.domain.report.dto.res;

import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.entity.ReportJob;

@Getter
@Builder
public class ReportGenerateStartResponse {

    private Long reportJobId;
    private String jobStatus;
    private String message;

    public static ReportGenerateStartResponse from(ReportJob reportJob) {
        return ReportGenerateStartResponse.builder()
                .reportJobId(reportJob.getJobId())
                .jobStatus(reportJob.getJobStatus().name())
                .message("보고서 생성 작업이 접수되었습니다.")
                .build();
    }
}
