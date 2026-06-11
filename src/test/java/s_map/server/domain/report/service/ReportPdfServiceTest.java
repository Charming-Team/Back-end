package s_map.server.domain.report.service;

import org.junit.jupiter.api.Test;
import s_map.server.domain.report.dto.res.ReportStructuredData;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportType;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReportPdfServiceTest {

    private final ReportPdfService reportPdfService = new ReportPdfService();

    @Test
    void generatePdfReturnsPdfBytes() {
        Report report = Report.builder()
                .reportId(1L)
                .reportTitle("2026년 6월 생산 보고서")
                .reportType(ReportType.MONTHLY)
                .authorId(10L)
                .targetStartDate(LocalDate.of(2026, 6, 1))
                .targetEndDate(LocalDate.of(2026, 6, 30))
                .build();
        ReportStructuredData structuredData = new ReportStructuredData(
                List.of(new ReportStructuredData.SummaryRow("보고서 기간", "2026.06.01 ~ 2026.06.30", "-")),
                List.of(new ReportStructuredData.LineRow("PE 범용 생산 Line", "91%", "12,000", "1.2%", "정상")),
                List.of(new ReportStructuredData.EquipmentRow("압출기", "88%", "2.1시간", "정상")),
                new ReportStructuredData.Analysis(
                        "전체 생산 흐름은 안정적입니다.",
                        List.of(new ReportStructuredData.AnalysisSection(
                                "납기 위험 분석",
                                List.of("위험 주문을 우선 점검하세요.")
                        )),
                        "핵심 자재 입고 일정을 우선 확인하세요."
                )
        );

        byte[] result = reportPdfService.generatePdf(report, "관리자", structuredData);

        assertThat(result).isNotEmpty();
        assertThat(new String(result, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
