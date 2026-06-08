package s_map.server.domain.report.dto.fastapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportType;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FastApiBusinessReportGenerateRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fromIncludesSourceReportSnapshot() throws Exception {
        Report report = Report.builder()
                .reportId(10L)
                .reportTitle("2026년 6월 수시 보고서")
                .reportType(ReportType.ON_DEMAND)
                .authorId(1L)
                .targetStartDate(LocalDate.of(2026, 6, 1))
                .targetEndDate(LocalDate.of(2026, 6, 14))
                .includedItems(objectMapper.readTree("""
                        {
                          "summaryRows": [
                            {
                              "label": "보고서 기간",
                              "value": "2026.06.01 ~ 2026.06.14",
                              "change": "-"
                            }
                          ]
                        }
                        """))
                .reportContent(objectMapper.readTree("""
                        {
                          "markdown": "## 주요 요약\\n- 생산 흐름은 안정적입니다."
                        }
                        """))
                .reportEvidence(objectMapper.readTree("""
                        [
                          {
                            "source": "production_plans"
                          }
                        ]
                        """))
                .relatedSimulationId(100L)
                .build();

        FastApiBusinessReportGenerateRequest request =
                FastApiBusinessReportGenerateRequest.from(report);

        assertThat(request.getReportId()).isEqualTo(10L);
        assertThat(request.getSourceReport().getReportId()).isEqualTo(10L);
        assertThat(request.getSourceReport().getReportTitle()).isEqualTo("2026년 6월 수시 보고서");
        assertThat(request.getSourceReport().getReportType()).isEqualTo("AD_HOC");
        assertThat(request.getSourceReport().getTargetStartDate()).isEqualTo("2026-06-01");
        assertThat(request.getSourceReport().getTargetEndDate()).isEqualTo("2026-06-14");
        assertThat(request.getSourceReport().getMarkdown()).contains("생산 흐름은 안정적입니다.");
        assertThat(request.getSourceReport().getSections().path("summaryRows").isArray()).isTrue();
        assertThat(request.getSourceReport().getReportContent().path("markdown").asText()).contains("주요 요약");
        assertThat(request.getSourceReport().getReportEvidence().get(0).path("source").asText())
                .isEqualTo("production_plans");
        assertThat(request.getSourceReport().getRelatedSimulationId()).isEqualTo(100L);
    }
}
