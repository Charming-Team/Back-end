package s_map.server.domain.report.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportType;
import s_map.server.domain.report.support.ReportPeriodSupport;
import s_map.server.domain.report.support.ReportPeriodSupport.ResolvedPeriod;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Schema(description = "보고서 상세 화면 표/분석 영역 구조화 데이터")
public record ReportStructuredData(
        List<SummaryRow> summaryRows,
        List<LineRow> lineRows,
        List<EquipmentRow> equipmentRows,
        Analysis analysis
) {

    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final int MAX_OVERVIEW_LENGTH = 180;
    private static final String MISSING_VALUE = "확인 필요";
    private static final String DEFAULT_RECOMMENDATION = "생성 필요";
    private static final Pattern ORDER_ITEM_PATTERN = Pattern.compile("주문\\s*(\\d+)");
    private static final Set<String> MARKDOWN_SECTION_HEADINGS = Set.of(
            "주요 요약",
            "라인별 성과",
            "주요 설비 현황",
            "보고서 요약 및 분석"
    );

    public static ReportStructuredData from(Report report, String markdown) {
        JsonNode structuredSource = findStructuredSource(report);

        List<SummaryRow> summaryRows = completeSummaryRows(
                parseSummaryRows(field(structuredSource, "summaryRows", "summary_rows")),
                report
        );
        List<LineRow> lineRows = completeLineRows(parseLineRows(field(structuredSource, "lineRows", "line_rows")));
        List<EquipmentRow> equipmentRows =
                completeEquipmentRows(parseEquipmentRows(field(structuredSource, "equipmentRows", "equipment_rows")));

        Analysis analysis = parseAnalysis(field(structuredSource, "analysis"));
        if (analysis == null) {
            analysis = createDefaultAnalysis(report, markdown);
        } else if (!hasText(analysis.overview())) {
            Analysis defaultAnalysis = createDefaultAnalysis(report, markdown);
            analysis = new Analysis(defaultAnalysis.overview(), analysis.sections(), analysis.recommendation());
        }

        return new ReportStructuredData(
                summaryRows,
                lineRows,
                equipmentRows,
                analysis
        );
    }

    private static JsonNode findStructuredSource(Report report) {
        if (report == null) {
            return null;
        }

        JsonNode includedItems = report.getIncludedItems();
        if (hasStructuredFields(includedItems)) {
            return includedItems;
        }

        JsonNode includedItemSections = field(includedItems, "sections");
        if (hasStructuredFields(includedItemSections)) {
            return includedItemSections;
        }

        JsonNode reportContent = report.getReportContent();
        if (hasStructuredFields(reportContent)) {
            return reportContent;
        }

        JsonNode reportContentSections = field(reportContent, "sections");
        if (hasStructuredFields(reportContentSections)) {
            return reportContentSections;
        }

        return null;
    }

    private static boolean hasStructuredFields(JsonNode node) {
        return node != null
                && node.isObject()
                && (hasField(node, "summaryRows", "summary_rows")
                || hasField(node, "lineRows", "line_rows")
                || hasField(node, "equipmentRows", "equipment_rows")
                || hasField(node, "analysis"));
    }

    private static List<SummaryRow> parseSummaryRows(JsonNode rowsNode) {
        if (rowsNode == null || !rowsNode.isArray()) {
            return List.of();
        }

        return stream(rowsNode)
                .map(row -> new SummaryRow(
                        text(row, "label"),
                        text(row, "value"),
                        defaultText(text(row, "change"), "-")
                ))
                .filter(row -> hasText(row.label()) || hasText(row.value()))
                .toList();
    }

    private static List<LineRow> parseLineRows(JsonNode rowsNode) {
        if (rowsNode == null || !rowsNode.isArray()) {
            return List.of();
        }

        return stream(rowsNode)
                .map(row -> new LineRow(
                        text(row, "line"),
                        text(row, "utilization"),
                        text(row, "completed"),
                        text(row, "defectRate", "defect_rate"),
                        normalizeStatusLabel(text(row, "note"))
                ))
                .filter(row -> hasText(row.line())
                        || hasText(row.utilization())
                        || hasText(row.completed())
                        || hasText(row.defectRate())
                        || hasText(row.note()))
                .toList();
    }

    private static List<EquipmentRow> parseEquipmentRows(JsonNode rowsNode) {
        if (rowsNode == null || !rowsNode.isArray()) {
            return List.of();
        }

        return stream(rowsNode)
                .map(row -> new EquipmentRow(
                        text(row, "name"),
                        text(row, "utilization"),
                        text(row, "downTime", "down_time"),
                        normalizeStatusLabel(text(row, "status"))
                ))
                .filter(row -> hasText(row.name())
                        || hasText(row.utilization())
                        || hasText(row.downTime())
                        || hasText(row.status()))
                .toList();
    }

    private static Analysis parseAnalysis(JsonNode analysisNode) {
        if (analysisNode == null || !analysisNode.isObject()) {
            return null;
        }

        List<AnalysisSection> sections = parseAnalysisSections(field(analysisNode, "sections"));
        String overview = emptyToNull(text(analysisNode, "overview"));
        String recommendation = emptyToNull(text(analysisNode, "recommendation"));

        if (!hasText(overview) && sections.isEmpty() && !hasText(recommendation)) {
            return null;
        }

        return new Analysis(overview, sections, defaultText(recommendation, DEFAULT_RECOMMENDATION));
    }

    private static List<AnalysisSection> parseAnalysisSections(JsonNode sectionsNode) {
        if (sectionsNode == null || !sectionsNode.isArray()) {
            return List.of();
        }

        return stream(sectionsNode)
                .map(section -> new AnalysisSection(
                        text(section, "title"),
                        deduplicateAnalysisItems(
                                text(section, "title"),
                                parseStringItems(field(section, "items"))
                        )
                ))
                .filter(section -> hasText(section.title()) || !section.items().isEmpty())
                .toList();
    }

    private static List<String> parseStringItems(JsonNode itemsNode) {
        if (itemsNode == null || itemsNode.isMissingNode() || itemsNode.isNull()) {
            return List.of();
        }

        if (itemsNode.isArray()) {
            LinkedHashSet<String> items = stream(itemsNode)
                    .map(ReportStructuredData::textValue)
                    .filter(ReportStructuredData::hasText)
                    .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
            return items.stream().toList();
        }

        String value = textValue(itemsNode);
        return hasText(value) ? List.of(value) : List.of();
    }

    private static List<String> deduplicateAnalysisItems(String title, List<String> items) {
        if (items.isEmpty()) {
            return items;
        }

        LinkedHashSet<String> keys = new LinkedHashSet<>();
        List<String> deduplicatedItems = new ArrayList<>();

        for (String item : items) {
            String key = analysisItemKey(title, item);
            if (keys.add(key)) {
                deduplicatedItems.add(item);
            }
        }

        return deduplicatedItems;
    }

    private static String analysisItemKey(String title, String item) {
        if (hasText(title) && title.contains("납기 위험")) {
            Matcher matcher = ORDER_ITEM_PATTERN.matcher(item);
            if (matcher.find()) {
                return "order:" + matcher.group(1);
            }
        }

        return item;
    }

    private static List<SummaryRow> createDefaultSummaryRows(Report report) {
        if (report == null) {
            return List.of();
        }

        return List.of(
                new SummaryRow("보고서 기간", formatReportPeriod(report), "-"),
                new SummaryRow("보고서 유형", reportTypeLabel(report.getReportType()), "-"),
                new SummaryRow("총 생산 계획 수량", MISSING_VALUE, "-"),
                new SummaryRow("총 생산 완료 수량", MISSING_VALUE, "-"),
                new SummaryRow("생산 계획 대비 실적", MISSING_VALUE, "-"),
                new SummaryRow("라인 가동률", MISSING_VALUE, "-"),
                new SummaryRow("평균 Cycle Time", MISSING_VALUE, "-"),
                new SummaryRow("불량 수량", MISSING_VALUE, "-"),
                new SummaryRow("불량률", MISSING_VALUE, "-"),
                new SummaryRow("설비 다운 타임", MISSING_VALUE, "-"),
                new SummaryRow("작업자 투입 시간", MISSING_VALUE, "-"),
                new SummaryRow("안전 사고 건수", MISSING_VALUE, "-"),
                new SummaryRow("납기 준수율", MISSING_VALUE, "-")
        );
    }

    private static List<SummaryRow> completeSummaryRows(List<SummaryRow> rows, Report report) {
        List<SummaryRow> defaultRows = createDefaultSummaryRows(report);
        if (rows.isEmpty()) {
            return defaultRows;
        }

        List<SummaryRow> completedRows = new ArrayList<>(rows.stream()
                .map(row -> normalizeSummaryRow(row, report))
                .toList());

        Set<String> labels = completedRows.stream()
                .map(SummaryRow::label)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        defaultRows.stream()
                .filter(row -> !labels.contains(row.label()))
                .forEach(completedRows::add);

        return completedRows;
    }

    private static SummaryRow normalizeSummaryRow(SummaryRow row, Report report) {
        String label = defaultText(row.label(), MISSING_VALUE);
        String value = defaultText(row.value(), MISSING_VALUE);

        if ("보고서 기간".equals(label)) {
            value = formatReportPeriod(report);
        } else if ("보고서 유형".equals(label) && report != null) {
            value = reportTypeLabel(report.getReportType());
        }

        return new SummaryRow(label, value, defaultText(row.change(), "-"));
    }

    private static List<LineRow> completeLineRows(List<LineRow> rows) {
        if (rows.isEmpty()) {
            return List.of(new LineRow(MISSING_VALUE, MISSING_VALUE, MISSING_VALUE, MISSING_VALUE, MISSING_VALUE));
        }

        return rows.stream()
                .map(row -> new LineRow(
                        defaultText(row.line(), MISSING_VALUE),
                        defaultText(row.utilization(), MISSING_VALUE),
                        defaultText(row.completed(), MISSING_VALUE),
                        defaultText(row.defectRate(), MISSING_VALUE),
                        defaultText(normalizeStatusLabel(row.note()), MISSING_VALUE)
                ))
                .toList();
    }

    private static List<EquipmentRow> completeEquipmentRows(List<EquipmentRow> rows) {
        if (rows.isEmpty()) {
            return List.of(new EquipmentRow(MISSING_VALUE, MISSING_VALUE, MISSING_VALUE, MISSING_VALUE));
        }

        return rows.stream()
                .map(row -> new EquipmentRow(
                        defaultText(row.name(), MISSING_VALUE),
                        defaultText(row.utilization(), MISSING_VALUE),
                        defaultText(row.downTime(), MISSING_VALUE),
                        defaultText(normalizeStatusLabel(row.status()), MISSING_VALUE)
                ))
                .toList();
    }

    private static Analysis createDefaultAnalysis(Report report, String markdown) {
        String overview = extractOverview(markdown);
        if (!hasText(overview) && report != null) {
            overview = report.getReportTitle();
        }
        if (!hasText(overview)) {
            overview = "분석 내용이 없습니다.";
        }

        return new Analysis(overview, List.of(), DEFAULT_RECOMMENDATION);
    }

    private static String extractOverview(String markdown) {
        if (!hasText(markdown)) {
            return null;
        }

        String[] lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String line : lines) {
            String normalized = normalizeMarkdownLine(line);
            if (hasText(normalized) && !MARKDOWN_SECTION_HEADINGS.contains(normalized)) {
                return truncate(normalized, MAX_OVERVIEW_LENGTH);
            }
        }

        return null;
    }

    private static String normalizeMarkdownLine(String line) {
        if (line == null) {
            return null;
        }

        return line.trim()
                .replaceFirst("^#{1,6}\\s*", "")
                .replaceFirst("^[-*+]\\s*", "")
                .replace("**", "")
                .replace("__", "")
                .replace("`", "")
                .trim();
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    private static String formatPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "-";
        }

        if (startDate == null) {
            return DISPLAY_DATE_FORMATTER.format(endDate);
        }

        if (endDate == null) {
            return DISPLAY_DATE_FORMATTER.format(startDate);
        }

        return DISPLAY_DATE_FORMATTER.format(startDate) + " ~ " + DISPLAY_DATE_FORMATTER.format(endDate);
    }

    private static String formatReportPeriod(Report report) {
        if (report == null) {
            return "-";
        }

        ResolvedPeriod period = ReportPeriodSupport.resolve(
                report.getReportType(),
                report.getTargetStartDate(),
                report.getTargetEndDate()
        );

        return formatPeriod(period.startDate(), period.endDate());
    }

    private static String reportTypeLabel(ReportType reportType) {
        if (reportType == null) {
            return "-";
        }

        return switch (reportType) {
            case MONTHLY, MONTHLY_BUSINESS -> "월간";
            case ON_DEMAND, ON_DEMAND_BUSINESS -> "수시";
        };
    }

    private static String text(JsonNode node, String fieldName) {
        return textValue(field(node, fieldName));
    }

    private static String text(JsonNode node, String camelFieldName, String snakeFieldName) {
        return textValue(field(node, camelFieldName, snakeFieldName));
    }

    private static String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        return node.asText().trim();
    }

    private static JsonNode field(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            return null;
        }

        return node.get(fieldName);
    }

    private static JsonNode field(JsonNode node, String camelFieldName, String snakeFieldName) {
        if (node == null || !node.isObject()) {
            return null;
        }

        JsonNode value = node.get(camelFieldName);
        return value != null ? value : node.get(snakeFieldName);
    }

    private static boolean hasField(JsonNode node, String fieldName) {
        return node != null && node.isObject() && node.has(fieldName);
    }

    private static boolean hasField(JsonNode node, String camelFieldName, String snakeFieldName) {
        return hasField(node, camelFieldName) || hasField(node, snakeFieldName);
    }

    private static String defaultText(String value, String defaultValue) {
        return hasText(value) ? value.trim() : defaultValue;
    }

    private static String emptyToNull(String value) {
        return hasText(value) ? value : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String normalizeStatusLabel(String status) {
        if (!hasText(status)) {
            return status;
        }

        String normalizedStatus = status.trim();
        String enumLikeStatus = normalizedStatus.toUpperCase()
                .replace("-", "_")
                .replace(" ", "_");

        return switch (enumLikeStatus) {
            case "RUNNING", "OPERATING", "ACTIVE" -> "정상";
            case "MAINTENANCE" -> "점검";
            case "STOPPED" -> "비가동";
            case "IDLE" -> "대기";
            case "SETUP" -> "전환/준비 중";
            case "ERROR" -> "오류";
            default -> normalizedStatus;
        };
    }

    private static Stream<JsonNode> stream(JsonNode arrayNode) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(arrayNode.elements(), Spliterator.ORDERED),
                false
        );
    }

    @Schema(description = "주요 요약 행")
    public record SummaryRow(
            @Schema(description = "요약 항목 라벨", example = "보고서 기간")
            String label,

            @Schema(description = "요약 항목 값", example = "2026.06.01 ~ 2026.06.14")
            String value,

            @Schema(description = "변동 또는 보조 표시", example = "-")
            String change
    ) {
    }

    @Schema(description = "라인별 성과 행")
    public record LineRow(
            @Schema(description = "라인명", example = "PP 범용 생산 Line")
            String line,

            @Schema(description = "가동률", example = "91%")
            String utilization,

            @Schema(description = "완료 수량", example = "12,000")
            String completed,

            @Schema(description = "불량률", example = "1.2%")
            String defectRate,

            @Schema(description = "비고", example = "정상")
            String note
    ) {
    }

    @Schema(description = "주요 설비 현황 행")
    public record EquipmentRow(
            @Schema(description = "설비명", example = "압출기")
            String name,

            @Schema(description = "가동률", example = "88%")
            String utilization,

            @Schema(description = "비가동 시간", example = "2.1시간")
            String downTime,

            @Schema(description = "설비 상태", example = "정상")
            String status
    ) {
    }

    @Schema(description = "보고서 요약 및 분석")
    public record Analysis(
            @Schema(description = "전체 요약 문장", example = "전체 생산 흐름은 안정적이나 일부 자재 리스크가 있습니다.")
            String overview,

            @Schema(description = "분석 섹션 목록")
            List<AnalysisSection> sections,

            @Schema(description = "권고 사항", example = "핵심 자재 입고 일정을 우선 확인하고 생산 순서를 조정하는 것이 필요합니다.")
            String recommendation
    ) {
    }

    @Schema(description = "분석 섹션")
    public record AnalysisSection(
            @Schema(description = "분석 섹션 제목", example = "자재 부족 분석")
            String title,

            @Schema(description = "분석 항목 목록")
            List<String> items
    ) {
    }
}
