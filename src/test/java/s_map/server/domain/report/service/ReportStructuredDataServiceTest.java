package s_map.server.domain.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import s_map.server.domain.report.dto.res.ReportStructuredData;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportType;
import s_map.server.domain.report.repository.ReportStructuredDataQueryRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportStructuredDataServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReportStructuredDataQueryRepository queryRepository =
            mock(ReportStructuredDataQueryRepository.class);
    private final ReportStructuredDataService service = new ReportStructuredDataService(queryRepository);

    @Test
    void createFromCurrentDatabaseBuildsStructuredRowsAndAnalysis() throws Exception {
        when(queryRepository.findSummaryMetrics(
                any(LocalDate.class),
                any(LocalDate.class),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        ))
                .thenReturn(new ReportStructuredDataQueryRepository.SummaryMetrics(
                        45,
                        29,
                        BigDecimal.valueOf(512900),
                        BigDecimal.valueOf(11817),
                        BigDecimal.valueOf(716),
                        BigDecimal.valueOf(128),
                        34,
                        2,
                        BigDecimal.valueOf(0.71),
                        10,
                        20,
                        18
                ));
        when(queryRepository.findLineRows(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(new ReportStructuredData.LineRow(
                        "ABS 보조 생산 Line",
                        "81%",
                        "9513",
                        "6.3%",
                        "정상"
                )));
        when(queryRepository.findEquipmentRows(any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(new ReportStructuredData.EquipmentRow(
                        "압출기",
                        "확인 필요",
                        "확인 필요",
                        "점검"
                )));

        ReportStructuredData structuredData = service.createFromCurrentDatabase(reportWithLegacySections());

        assertThat(structuredData.summaryRows()).contains(
                new ReportStructuredData.SummaryRow("총 주문 수", "45", "-"),
                new ReportStructuredData.SummaryRow("총 생산 계획 수량", "512900", "-"),
                new ReportStructuredData.SummaryRow("생산 계획 대비 실적", "2.3%", "-"),
                new ReportStructuredData.SummaryRow("라인 가동률", "71%", "-"),
                new ReportStructuredData.SummaryRow("납기 준수율", "90%", "-")
        );
        assertThat(structuredData.lineRows()).containsExactly(new ReportStructuredData.LineRow(
                "ABS 보조 생산 Line",
                "81%",
                "9513",
                "6.3%",
                "정상"
        ));
        assertThat(structuredData.equipmentRows()).containsExactly(new ReportStructuredData.EquipmentRow(
                "압출기",
                "확인 필요",
                "확인 필요",
                "점검"
        ));
        assertThat(structuredData.analysis()).isEqualTo(new ReportStructuredData.Analysis(
                "현재 미완료 생산계획과 미래 주문 중 자재 부족을 분석했습니다.",
                List.of(new ReportStructuredData.AnalysisSection(
                        "핵심 리스크",
                        List.of("Foaming Agent 입고 지연 위험")
                )),
                "생산 순서를 조정하세요."
        ));
    }

    private Report reportWithLegacySections() throws Exception {
        return Report.builder()
                .reportId(3L)
                .reportTitle("자재 부족 및 입고 지연 리스크 분석 보고서")
                .reportType(ReportType.ON_DEMAND)
                .authorId(1L)
                .targetStartDate(LocalDate.of(2026, 6, 1))
                .targetEndDate(LocalDate.of(2026, 6, 14))
                .includedItems(null)
                .reportContent(objectMapper.readTree("""
                        {
                          "sections": [
                            {
                              "section_key": "summary",
                              "section_title": "요약",
                              "content": "현재 미완료 생산계획과 미래 주문 중 자재 부족을 분석했습니다."
                            },
                            {
                              "section_key": "risk",
                              "section_title": "핵심 리스크",
                              "content": "- Foaming Agent 입고 지연 위험"
                            },
                            {
                              "section_key": "recommended_actions",
                              "section_title": "종합 의견 및 제안",
                              "content": "생산 순서를 조정하세요."
                            }
                          ]
                        }
                        """))
                .reportEvidence(objectMapper.createArrayNode())
                .relatedSimulationId(null)
                .build();
    }
}
