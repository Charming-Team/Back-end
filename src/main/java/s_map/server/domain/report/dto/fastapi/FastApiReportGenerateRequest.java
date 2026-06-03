package s_map.server.domain.report.dto.fastapi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.dto.req.ReportGenerateRequest;

@Getter
@Builder
@Schema(description = "FastAPI 보고서 생성 요청")
public class FastApiReportGenerateRequest {

    @Schema(description = "보고서 생성 Job ID", example = "1")
    private Long reportJobId;

    @Schema(description = "보고서 생성 요청 사용자 ID", example = "1")
    private Long requestedBy;

    @Schema(description = "보고서 생성 요청 사용자 권한", example = "MANUFACTURING_MANAGER")
    private String userRole;

    @Schema(description = "보고서 유형", example = "MONTHLY")
    private String reportType;

    @Schema(description = "보고서 대상 기간")
    private FastApiReportPeriodRequest period;

    @Schema(description = "주요 요약 포함 여부", example = "true")
    private Boolean includeExecutiveSummary;

    @Schema(description = "생성 근거 데이터 포함 여부", example = "true")
    private Boolean includeEvidence;

    public static FastApiReportGenerateRequest of(
            Long reportJobId,
            Long requestedBy,
            String userRole,
            ReportGenerateRequest request
    ) {
        return FastApiReportGenerateRequest.builder()
                .reportJobId(reportJobId)
                .requestedBy(requestedBy)
                .userRole(userRole)
                .reportType(request.getReportType().name())
                .period(FastApiReportPeriodRequest.builder()
                        .startDate(request.getPeriod().getStartDate())
                        .endDate(request.getPeriod().getEndDate())
                        .build())
                .includeExecutiveSummary(request.getIncludeExecutiveSummary())
                .includeEvidence(request.getIncludeEvidence())
                .build();
    }
}
