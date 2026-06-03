package s_map.server.domain.report.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "보고서 생성 Job 상태")
public enum ReportJobStatus {
    @Schema(description = "생성 작업 접수")
    PENDING,

    @Schema(description = "생성 작업 진행 중")
    RUNNING,

    @Schema(description = "생성 성공")
    SUCCESS,

    @Schema(description = "생성 실패")
    FAILED
}
