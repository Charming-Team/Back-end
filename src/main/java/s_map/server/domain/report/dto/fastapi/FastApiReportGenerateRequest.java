package s_map.server.domain.report.dto.fastapi;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.dto.req.ReportGenerateRequest;
import s_map.server.domain.report.entity.ReportType;
import s_map.server.domain.report.support.ReportPeriodSupport;
import s_map.server.domain.report.support.ReportPeriodSupport.ResolvedPeriod;

import java.util.Locale;

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
        ResolvedPeriod period = ReportPeriodSupport.resolve(
                request.getReportType(),
                request.getPeriod().getStartDate(),
                request.getPeriod().getEndDate()
        );

        return FastApiReportGenerateRequest.builder()
                .reportJobId(reportJobId)
                .requestedBy(requestedBy)
                .userRole(toFastApiUserRole(userRole))
                .reportType(toFastApiReportType(request.getReportType()))
                .period(FastApiReportPeriodRequest.builder()
                        .startDate(period.startDate())
                        .endDate(period.endDate())
                        .build())
                .includeExecutiveSummary(defaultTrue(request.getIncludeExecutiveSummary()))
                .includeEvidence(defaultTrue(request.getIncludeEvidence()))
                .build();
    }

    private static Boolean defaultTrue(Boolean value) {
        return value != null ? value : Boolean.TRUE;
    }

    private static String toFastApiReportType(ReportType reportType) {
        if (reportType == null) {
            return null;
        }

        return switch (reportType) {
            case ON_DEMAND, ON_DEMAND_BUSINESS -> "AD_HOC";
            case MONTHLY, MONTHLY_BUSINESS -> "MONTHLY";
        };
    }

    private static String toFastApiUserRole(String userRole) {
        if (userRole == null || userRole.isBlank()) {
            return userRole;
        }

        String normalizedUserRole = userRole.trim()
                .toUpperCase(Locale.ROOT)
                .replace("-", "_")
                .replace(" ", "_");

        return switch (normalizedUserRole) {
            case "MANUFACTURING_MANAGER" -> "PRODUCTION_MANAGER";
            case "OPERATOR" -> "WORKER";
            default -> normalizedUserRole;
        };
    }
}
