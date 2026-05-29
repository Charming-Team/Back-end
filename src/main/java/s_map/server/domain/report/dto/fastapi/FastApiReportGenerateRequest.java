package s_map.server.domain.report.dto.fastapi;

import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.dto.req.ReportGenerateRequest;

@Getter
@Builder
public class FastApiReportGenerateRequest {

    private Long reportJobId;
    private Long requestedBy;
    private String userRole;
    private String reportType;
    private FastApiReportPeriodRequest period;
    private Boolean includeExecutiveSummary;
    private Boolean includeEvidence;

    public static FastApiReportGenerateRequest of(Long reportJobId, ReportGenerateRequest request) {
        return FastApiReportGenerateRequest.builder()
                .reportJobId(reportJobId)
                .requestedBy(request.getRequestedBy())
                .userRole(request.getUserRole())
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