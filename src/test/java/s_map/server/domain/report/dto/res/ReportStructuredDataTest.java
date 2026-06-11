package s_map.server.domain.report.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportType;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportStructuredDataTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fromUsesStructuredFieldsFromIncludedItems() throws Exception {
        JsonNode includedItems = objectMapper.readTree("""
                {
                  "summaryRows": [
                    {
                      "label": "보고서 기간",
                      "value": "2026.06.01 ~ 2026.06.14",
                      "change": "-"
                    }
                  ],
                  "lineRows": [
                    {
                      "line": "PP 범용 생산 Line",
                      "utilization": "91%",
                      "completed": "12,000",
                      "defect_rate": "1.2%",
                      "note": "정상"
                    }
                  ],
                  "equipment_rows": [
                    {
                      "name": "압출기",
                      "utilization": "88%",
                      "down_time": "2.1시간",
                      "status": "정상"
                    }
                  ],
                  "analysis": {
                    "overview": "전체 생산 흐름은 안정적입니다.",
                    "sections": [
                      {
                        "title": "자재 부족 분석",
                        "items": [
                          "Foaming Agent 재고 부족으로 납기 위험이 있습니다."
                        ]
                      }
                    ],
                    "recommendation": "핵심 자재 입고 일정을 우선 확인하세요."
                  }
                }
                """);

        ReportStructuredData structuredData = ReportStructuredData.from(
                report(includedItems),
                "## 주요 요약\n- 기존 본문"
        );

        assertThat(structuredData.summaryRows()).startsWith(
                new ReportStructuredData.SummaryRow("보고서 기간", "2026.06.01 ~ 2026.06.14", "-")
        );
        assertThat(structuredData.summaryRows())
                .extracting(ReportStructuredData.SummaryRow::label)
                .contains(
                        "보고서 유형",
                        "총 생산 계획 수량",
                        "총 생산 완료 수량",
                        "생산 계획 대비 실적",
                        "라인 가동률",
                        "평균 Cycle Time",
                        "불량 수량",
                        "불량률",
                        "설비 다운 타임",
                        "작업자 투입 시간",
                        "안전 사고 건수",
                        "납기 준수율"
                );
        assertThat(structuredData.lineRows()).containsExactly(
                new ReportStructuredData.LineRow("PP 범용 생산 Line", "91%", "12,000", "1.2%", "정상")
        );
        assertThat(structuredData.equipmentRows()).containsExactly(
                new ReportStructuredData.EquipmentRow("압출기", "88%", "2.1시간", "정상")
        );
        assertThat(structuredData.analysis()).isEqualTo(
                new ReportStructuredData.Analysis(
                        "전체 생산 흐름은 안정적입니다.",
                        List.of(new ReportStructuredData.AnalysisSection(
                                "자재 부족 분석",
                                List.of("Foaming Agent 재고 부족으로 납기 위험이 있습니다.")
                        )),
                        "핵심 자재 입고 일정을 우선 확인하세요."
                )
        );
    }

    @Test
    void fromCreatesFallbackSummaryAndAnalysisWhenStructuredFieldsAreMissing() throws Exception {
        ReportStructuredData structuredData = ReportStructuredData.from(
                report(objectMapper.createObjectNode()),
                "## 주요 요약\n- 전체 생산 흐름은 안정적입니다.\n\n## 라인별 성과"
        );

        assertThat(structuredData.summaryRows()).containsExactly(
                new ReportStructuredData.SummaryRow("보고서 기간", "2026.06.01 ~ 2026.06.14", "-"),
                new ReportStructuredData.SummaryRow("보고서 유형", "수시", "-"),
                new ReportStructuredData.SummaryRow("총 생산 계획 수량", "확인 필요", "-"),
                new ReportStructuredData.SummaryRow("총 생산 완료 수량", "확인 필요", "-"),
                new ReportStructuredData.SummaryRow("생산 계획 대비 실적", "확인 필요", "-"),
                new ReportStructuredData.SummaryRow("라인 가동률", "확인 필요", "-"),
                new ReportStructuredData.SummaryRow("평균 Cycle Time", "확인 필요", "-"),
                new ReportStructuredData.SummaryRow("불량 수량", "확인 필요", "-"),
                new ReportStructuredData.SummaryRow("불량률", "확인 필요", "-"),
                new ReportStructuredData.SummaryRow("설비 다운 타임", "확인 필요", "-"),
                new ReportStructuredData.SummaryRow("작업자 투입 시간", "확인 필요", "-"),
                new ReportStructuredData.SummaryRow("안전 사고 건수", "확인 필요", "-"),
                new ReportStructuredData.SummaryRow("납기 준수율", "확인 필요", "-")
        );
        assertThat(structuredData.lineRows()).containsExactly(
                new ReportStructuredData.LineRow("확인 필요", "확인 필요", "확인 필요", "확인 필요", "확인 필요")
        );
        assertThat(structuredData.equipmentRows()).containsExactly(
                new ReportStructuredData.EquipmentRow("확인 필요", "확인 필요", "확인 필요", "확인 필요")
        );
        assertThat(structuredData.analysis()).isEqualTo(
                new ReportStructuredData.Analysis("전체 생산 흐름은 안정적입니다.", List.of(), "생성 필요")
        );
    }

    @Test
    void fromNormalizesMonthlyPeriodStatusLabelsAndDuplicateAnalysisItems() throws Exception {
        JsonNode includedItems = objectMapper.readTree("""
                {
                  "summaryRows": [
                    {
                      "label": "보고서 기간",
                      "value": "2026-05-01 ~ 2026-06-11",
                      "change": "-"
                    },
                    {
                      "label": "보고서 유형",
                      "value": "월간",
                      "change": "-"
                    }
                  ],
                  "lineRows": [
                    {
                      "line": "LINE-PE-01 PE 범용 생산 Line",
                      "utilization": "54%",
                      "completed": "0",
                      "defectRate": "-",
                      "note": "SETUP"
                    }
                  ],
                  "equipmentRows": [
                    {
                      "name": "M-PP02-EXT PP-02 압출기",
                      "utilization": "확인 필요",
                      "downTime": "확인 필요",
                      "status": " 점검"
                    }
                  ],
                  "analysis": {
                    "overview": "월간 생산 운영 요약입니다.",
                    "sections": [
                      {
                        "title": "납기 위험 분석",
                        "items": [
                          "주문 391는 CRITICAL 위험으로 분류되었고, 예상 지연일은 11.46일입니다.",
                          "주문 391는 CRITICAL 위험으로 분류되었고, 예상 지연일은 10.8일입니다."
                        ]
                      }
                    ],
                    "recommendation": "위험 주문을 우선 점검하세요."
                  }
                }
                """);

        ReportStructuredData structuredData = ReportStructuredData.from(
                monthlyReport(includedItems),
                "## 주요 요약\n- 월간 생산 운영 요약입니다."
        );

        assertThat(structuredData.summaryRows()).startsWith(
                new ReportStructuredData.SummaryRow("보고서 기간", "2026.05.01 ~ 2026.05.31", "-"),
                new ReportStructuredData.SummaryRow("보고서 유형", "월간", "-")
        );
        assertThat(structuredData.lineRows()).containsExactly(new ReportStructuredData.LineRow(
                "LINE-PE-01 PE 범용 생산 Line",
                "54%",
                "0",
                "-",
                "전환/준비 중"
        ));
        assertThat(structuredData.equipmentRows()).containsExactly(new ReportStructuredData.EquipmentRow(
                "M-PP02-EXT PP-02 압출기",
                "확인 필요",
                "확인 필요",
                "점검"
        ));
        assertThat(structuredData.analysis().sections().get(0).items())
                .containsExactly("주문 391는 CRITICAL 위험으로 분류되었고, 예상 지연일은 11.46일입니다.");
    }

    private Report report(JsonNode includedItems) throws Exception {
        return Report.builder()
                .reportId(1L)
                .reportTitle("2026년 6월 수시 보고서")
                .reportType(ReportType.ON_DEMAND)
                .authorId(1L)
                .targetStartDate(LocalDate.of(2026, 6, 1))
                .targetEndDate(LocalDate.of(2026, 6, 14))
                .includedItems(includedItems)
                .reportContent(objectMapper.readTree("""
                        {
                          "markdown": "## 주요 요약\\n- 전체 생산 흐름은 안정적입니다."
                        }
                        """))
                .reportEvidence(objectMapper.createArrayNode())
                .relatedSimulationId(null)
                .build();
    }

    private Report monthlyReport(JsonNode includedItems) throws Exception {
        return Report.builder()
                .reportId(2L)
                .reportTitle("2026년 05월 생산 운영 보고서")
                .reportType(ReportType.MONTHLY)
                .authorId(1L)
                .targetStartDate(LocalDate.of(2026, 5, 1))
                .targetEndDate(LocalDate.of(2026, 6, 11))
                .includedItems(includedItems)
                .reportContent(objectMapper.readTree("""
                        {
                          "markdown": "## 주요 요약\\n- 월간 생산 운영 요약입니다."
                        }
                        """))
                .reportEvidence(objectMapper.createArrayNode())
                .relatedSimulationId(null)
                .build();
    }
}
