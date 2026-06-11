package s_map.server.domain.report.dto.fastapi;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import s_map.server.domain.report.dto.req.ReportGenerateRequest;
import s_map.server.domain.report.dto.req.ReportPeriodRequest;
import s_map.server.domain.report.entity.ReportType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FastApiReportGenerateRequestTest {

    @Test
    void ofMapsSpringEnumsToFastApiEnums() {
        ReportGenerateRequest source = reportGenerateRequest(ReportType.ON_DEMAND);

        FastApiReportGenerateRequest request = FastApiReportGenerateRequest.of(
                13L,
                1L,
                "MANUFACTURING_MANAGER",
                source
        );

        assertThat(request.getReportJobId()).isEqualTo(13L);
        assertThat(request.getRequestedBy()).isEqualTo(1L);
        assertThat(request.getUserRole()).isEqualTo("PRODUCTION_MANAGER");
        assertThat(request.getReportType()).isEqualTo("AD_HOC");
        assertThat(request.getPeriod().getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(request.getPeriod().getEndDate()).isEqualTo(LocalDate.of(2026, 6, 14));
        assertThat(request.getIncludeExecutiveSummary()).isTrue();
        assertThat(request.getIncludeEvidence()).isTrue();
    }

    @Test
    void ofMapsOperatorRoleToWorker() {
        ReportGenerateRequest source = reportGenerateRequest(ReportType.MONTHLY);

        FastApiReportGenerateRequest request = FastApiReportGenerateRequest.of(
                14L,
                2L,
                "OPERATOR",
                source
        );

        assertThat(request.getUserRole()).isEqualTo("WORKER");
        assertThat(request.getReportType()).isEqualTo("MONTHLY");
        assertThat(request.getPeriod().getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(request.getPeriod().getEndDate()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    private ReportGenerateRequest reportGenerateRequest(ReportType reportType) {
        ReportGenerateRequest request = new ReportGenerateRequest();
        ReflectionTestUtils.setField(request, "reportType", reportType);
        ReflectionTestUtils.setField(request, "period", period());
        return request;
    }

    private ReportPeriodRequest period() {
        ReportPeriodRequest period = new ReportPeriodRequest();
        ReflectionTestUtils.setField(period, "startDate", LocalDate.of(2026, 6, 1));
        ReflectionTestUtils.setField(period, "endDate", LocalDate.of(2026, 6, 14));
        return period;
    }
}
