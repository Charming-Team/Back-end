package s_map.server.domain.report.dto.req;

import lombok.Getter;
import s_map.server.domain.report.entity.ReportType;

@Getter
public class ReportGenerateRequest {

    private Long requestedBy;
    private String userRole;
    private ReportType reportType;
    private ReportPeriodRequest period;
    private Boolean includeExecutiveSummary;
    private Boolean includeEvidence;
}