package s_map.server.domain.report.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import s_map.server.domain.report.dto.res.ReportStructuredData;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportType;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ReportPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");
    private static final String MISSING_VALUE = "확인 필요";
    private static final Color HEADER_BACKGROUND = new Color(236, 240, 246);
    private static final List<String> FONT_CANDIDATE_PATHS = List.of(
            "/usr/share/fonts/noto-cjk/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansKR-Regular.otf",
            "/System/Library/Fonts/AppleSDGothicNeo.ttc",
            "/Library/Fonts/AppleGothic.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
    );

    /**
     * 기능: 저장된 보고서 상세 데이터를 PDF byte 배열로 렌더링한다.
     *
     * Input:
     * - report / Report / PDF 생성 대상 보고서 최신 저장 row
     * - authorName / String / 작성자 표시명
     * - structuredData / ReportStructuredData / 상세 화면 표/분석 구조화 데이터
     *
     * Output:
     * - byte[] / 다운로드로 제공할 PDF 파일 내용
     */
    public byte[] generatePdf(
            Report report,
            String authorName,
            ReportStructuredData structuredData
    ) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 42, 42);
            PdfWriter.getInstance(document, outputStream);

            PdfFonts fonts = createFonts();
            document.addTitle(valueOrMissing(report.getReportTitle()));
            document.addAuthor(valueOrMissing(authorName));
            document.open();

            addTitle(document, report, fonts);
            addMetadata(document, report, authorName, fonts);
            addSummaryRows(document, structuredData.summaryRows(), fonts);
            addLineRows(document, structuredData.lineRows(), fonts);
            addEquipmentRows(document, structuredData.equipmentRows(), fonts);
            addAnalysis(document, structuredData.analysis(), fonts);

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException exception) {
            log.error(
                    "[ReportPdfService] PDF 생성 실패 reportId={}",
                    report == null ? null : report.getReportId(),
                    exception
            );
            throw new IllegalStateException("PDF generation failed.", exception);
        }
    }

    private void addTitle(Document document, Report report, PdfFonts fonts) throws DocumentException {
        Paragraph title = new Paragraph(valueOrMissing(report.getReportTitle()), fonts.title());
        title.setAlignment(Element.ALIGN_LEFT);
        title.setSpacingAfter(8);
        document.add(title);

        Paragraph subtitle = new Paragraph("보고서 PDF 출력", fonts.subtitle());
        subtitle.setSpacingAfter(18);
        document.add(subtitle);
    }

    private void addMetadata(
            Document document,
            Report report,
            String authorName,
            PdfFonts fonts
    ) throws DocumentException {
        PdfPTable table = createTable(new float[]{1.2f, 2.0f, 1.2f, 2.0f});
        addHeaderCell(table, "작성자", fonts);
        addBodyCell(table, valueOrMissing(authorName), fonts);
        addHeaderCell(table, "작성일", fonts);
        addBodyCell(table, formatDateTime(report.getCreatedAt()), fonts);
        addHeaderCell(table, "보고서 유형", fonts);
        addBodyCell(table, reportTypeLabel(report.getReportType()), fonts);
        addHeaderCell(table, "대상 기간", fonts);
        addBodyCell(table, formatPeriod(report.getTargetStartDate(), report.getTargetEndDate()), fonts);
        addHeaderCell(table, "최종 수정일", fonts);
        addBodyCell(table, formatDateTime(report.getUpdatedAt()), fonts);
        addHeaderCell(table, "보고서 ID", fonts);
        addBodyCell(table, String.valueOf(report.getReportId()), fonts);
        table.setSpacingAfter(16);
        document.add(table);
    }

    private void addSummaryRows(
            Document document,
            List<ReportStructuredData.SummaryRow> rows,
            PdfFonts fonts
    ) throws DocumentException {
        addSectionTitle(document, "주요 요약", fonts);
        PdfPTable table = createTable(new float[]{1.5f, 2.4f, 1.1f});
        addHeaderCell(table, "항목", fonts);
        addHeaderCell(table, "값", fonts);
        addHeaderCell(table, "변화", fonts);

        for (ReportStructuredData.SummaryRow row : safeList(rows)) {
            addBodyCell(table, valueOrMissing(row.label()), fonts);
            addBodyCell(table, valueOrMissing(row.value()), fonts);
            addBodyCell(table, valueOrMissing(row.change()), fonts);
        }

        table.setSpacingAfter(16);
        document.add(table);
    }

    private void addLineRows(
            Document document,
            List<ReportStructuredData.LineRow> rows,
            PdfFonts fonts
    ) throws DocumentException {
        addSectionTitle(document, "라인별 성과", fonts);
        PdfPTable table = createTable(new float[]{2.0f, 1.0f, 1.2f, 1.0f, 1.3f});
        addHeaderCell(table, "라인", fonts);
        addHeaderCell(table, "가동률", fonts);
        addHeaderCell(table, "완료 수량", fonts);
        addHeaderCell(table, "불량률", fonts);
        addHeaderCell(table, "비고", fonts);

        for (ReportStructuredData.LineRow row : safeList(rows)) {
            addBodyCell(table, valueOrMissing(row.line()), fonts);
            addBodyCell(table, valueOrMissing(row.utilization()), fonts);
            addBodyCell(table, valueOrMissing(row.completed()), fonts);
            addBodyCell(table, valueOrMissing(row.defectRate()), fonts);
            addBodyCell(table, valueOrMissing(row.note()), fonts);
        }

        table.setSpacingAfter(16);
        document.add(table);
    }

    private void addEquipmentRows(
            Document document,
            List<ReportStructuredData.EquipmentRow> rows,
            PdfFonts fonts
    ) throws DocumentException {
        addSectionTitle(document, "주요 설비 현황", fonts);
        PdfPTable table = createTable(new float[]{2.0f, 1.1f, 1.3f, 1.2f});
        addHeaderCell(table, "설비", fonts);
        addHeaderCell(table, "가동률", fonts);
        addHeaderCell(table, "다운타임", fonts);
        addHeaderCell(table, "상태", fonts);

        for (ReportStructuredData.EquipmentRow row : safeList(rows)) {
            addBodyCell(table, valueOrMissing(row.name()), fonts);
            addBodyCell(table, valueOrMissing(row.utilization()), fonts);
            addBodyCell(table, valueOrMissing(row.downTime()), fonts);
            addBodyCell(table, valueOrMissing(row.status()), fonts);
        }

        table.setSpacingAfter(16);
        document.add(table);
    }

    private void addAnalysis(
            Document document,
            ReportStructuredData.Analysis analysis,
            PdfFonts fonts
    ) throws DocumentException {
        addSectionTitle(document, "보고서 요약 및 분석", fonts);
        if (analysis == null) {
            addParagraph(document, MISSING_VALUE, fonts.body(), 8);
            return;
        }

        addParagraph(document, valueOrMissing(analysis.overview()), fonts.body(), 8);

        for (ReportStructuredData.AnalysisSection section : safeList(analysis.sections())) {
            addParagraph(document, valueOrMissing(section.title()), fonts.heading(), 4);
            for (String item : safeList(section.items())) {
                addParagraph(document, "- " + valueOrMissing(item), fonts.body(), 3);
            }
        }

        addParagraph(document, "종합 의견 및 제안", fonts.heading(), 4);
        addParagraph(document, valueOrMissing(analysis.recommendation()), fonts.body(), 0);
    }

    private void addSectionTitle(
            Document document,
            String title,
            PdfFonts fonts
    ) throws DocumentException {
        Paragraph paragraph = new Paragraph(title, fonts.sectionTitle());
        paragraph.setSpacingBefore(4);
        paragraph.setSpacingAfter(8);
        document.add(paragraph);
    }

    private void addParagraph(
            Document document,
            String text,
            Font font,
            int spacingAfter
    ) throws DocumentException {
        Paragraph paragraph = new Paragraph(valueOrMissing(text), font);
        paragraph.setLeading(15);
        paragraph.setSpacingAfter(spacingAfter);
        document.add(paragraph);
    }

    private PdfPTable createTable(float[] widths) throws DocumentException {
        PdfPTable table = new PdfPTable(widths);
        table.setWidthPercentage(100);
        return table;
    }

    private void addHeaderCell(PdfPTable table, String value, PdfFonts fonts) {
        PdfPCell cell = createCell(value, fonts.tableHeader());
        cell.setBackgroundColor(HEADER_BACKGROUND);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String value, PdfFonts fonts) {
        table.addCell(createCell(value, fonts.tableBody()));
    }

    private PdfPCell createCell(String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(valueOrMissing(value), font));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(Color.WHITE);
        return cell;
    }

    private PdfFonts createFonts() throws DocumentException, IOException {
        BaseFont baseFont = resolveBaseFont();
        return new PdfFonts(
                new Font(baseFont, 18, Font.BOLD),
                new Font(baseFont, 10, Font.NORMAL, Color.DARK_GRAY),
                new Font(baseFont, 12, Font.BOLD),
                new Font(baseFont, 11, Font.BOLD),
                new Font(baseFont, 9, Font.BOLD),
                new Font(baseFont, 9, Font.NORMAL)
        );
    }

    private BaseFont resolveBaseFont() throws DocumentException, IOException {
        for (String candidatePath : FONT_CANDIDATE_PATHS) {
            Path path = Path.of(candidatePath);
            if (!Files.exists(path)) {
                continue;
            }

            String fontPath = candidatePath.endsWith(".ttc") ? candidatePath + ",0" : candidatePath;
            return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        }

        log.warn("[ReportPdfService] 한글 PDF 폰트를 찾지 못해 기본 폰트로 PDF를 생성합니다.");
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
    }

    private String reportTypeLabel(ReportType reportType) {
        if (reportType == null) {
            return MISSING_VALUE;
        }

        return switch (reportType) {
            case MONTHLY -> "월간 보고서";
            case ON_DEMAND -> "수시 보고서";
            case MONTHLY_BUSINESS -> "월간 경영진용 보고서";
            case ON_DEMAND_BUSINESS -> "수시 경영진용 보고서";
        };
    }

    private String formatPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return MISSING_VALUE;
        }
        if (startDate == null) {
            return DATE_FORMATTER.format(endDate);
        }
        if (endDate == null) {
            return DATE_FORMATTER.format(startDate);
        }
        return DATE_FORMATTER.format(startDate) + " ~ " + DATE_FORMATTER.format(endDate);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? MISSING_VALUE : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String valueOrMissing(String value) {
        return value == null || value.isBlank() ? MISSING_VALUE : value.trim();
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record PdfFonts(
            Font title,
            Font subtitle,
            Font sectionTitle,
            Font heading,
            Font tableHeader,
            Font tableBody
    ) {
        Font body() {
            return tableBody;
        }
    }
}
