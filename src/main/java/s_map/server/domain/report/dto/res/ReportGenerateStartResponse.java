package s_map.server.domain.report.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.entity.ReportJob;

@Getter
@Builder
@Schema(description = "보고서 생성 작업 접수 응답")
public class ReportGenerateStartResponse {

    @Schema(description = "보고서 생성 Job ID", example = "1")
    private Long reportJobId;

    @Schema(description = "보고서 생성 Job 상태", example = "PENDING")
    private String jobStatus;

    @Schema(description = "보고서 생성 접수 메시지", example = "보고서 생성 작업이 접수되었습니다.")
    private String message;

    public static ReportGenerateStartResponse from(ReportJob reportJob) {
        return ReportGenerateStartResponse.builder()
                .reportJobId(reportJob.getJobId())
                .jobStatus(reportJob.getJobStatus().name())
                .message("보고서 생성 작업이 접수되었습니다.")
                .build();
    }
}
