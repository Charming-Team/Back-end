package s_map.server.domain.report.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.entity.ReportJob;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "보고서 생성 Job 조회 응답")
public class ReportJobResponse {

    @Schema(description = "보고서 생성 Job ID", example = "1")
    private Long reportJobId;

    @Schema(description = "생성 완료된 보고서 ID", example = "10", nullable = true)
    private Long reportId;

    @Schema(description = "보고서 생성 요청 사용자 ID", example = "1")
    private Long requestedBy;

    @Schema(description = "보고서 생성 Job 상태", example = "RUNNING")
    private String jobStatus;

    @Schema(description = "보고서 생성 실패 사유", example = "보고서 생성에 실패했습니다.", nullable = true)
    private String errorMessage;

    @Schema(description = "보고서 생성 재시도 횟수", example = "0")
    private Integer retryCount;

    @Schema(description = "보고서 생성 시작 일시", example = "2026-06-03T10:00:00", nullable = true)
    private LocalDateTime startedAt;

    @Schema(description = "보고서 생성 완료 또는 실패 일시", example = "2026-06-03T10:01:30", nullable = true)
    private LocalDateTime finishedAt;

    @Schema(description = "Job 생성 일시", example = "2026-06-03T09:59:59")
    private LocalDateTime createdAt;

    @Schema(description = "Job 수정 일시", example = "2026-06-03T10:01:30")
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
