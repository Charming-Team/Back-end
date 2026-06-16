package s_map.server.domain.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import s_map.server.domain.report.dto.res.ReportStructuredData;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportType;
import s_map.server.domain.report.repository.ReportStructuredDataQueryRepository;
import s_map.server.domain.report.support.ReportPeriodSupport;
import s_map.server.domain.report.support.ReportPeriodSupport.ResolvedPeriod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportStructuredDataService {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");
    private static final String MISSING_VALUE = "확인 필요";

    private final ReportStructuredDataQueryRepository queryRepository;

    /**
     * 기능: 저장된 보고서의 구조화 데이터를 조회하거나 현재 DB 기준으로 보완한다.
     *
     * Input:
     * - report / Report / 구조화 데이터를 구성할 보고서 엔티티
     *
     * Output:
     * - result / ReportStructuredData / 보고서 상세 화면과 PDF에 사용할 구조화 데이터
     */
    public ReportStructuredData resolve(Report report) {
        String markdown = extractMarkdown(report.getReportContent());
        if (hasStructuredData(report)) {
            return enrichStructuredData(report, ReportStructuredData.from(report, markdown));
        }

        return createFromCurrentDatabase(report);
    }

    /**
     * 기능: 보고서 기간과 유형을 기준으로 현재 DB 데이터에서 보고서 구조화 데이터를 생성한다.
     *
     * Input:
     * - report / Report / 보고서 유형과 대상 기간을 포함한 보고서 엔티티
     *
     * Output:
     * - result / ReportStructuredData / 현재 DB 기준 주요 요약, 라인별 성과, 설비 현황, 분석 데이터
     */
    public ReportStructuredData createFromCurrentDatabase(Report report) {
        DateRange dateRange = DateRange.from(
                report.getReportType(),
                report.getTargetStartDate(),
                report.getTargetEndDate()
        );
        ReportStructuredDataQueryRepository.SummaryMetrics metrics =
                queryRepository.findSummaryMetrics(
                        dateRange.startDate(),
                        dateRange.endDateExclusive(),
                        dateRange.startAt(),
                        dateRange.endExclusive()
                );

        List<ReportStructuredData.LineRow> lineRows =
                queryRepository.findLineRows(dateRange.startAt(), dateRange.endExclusive());
        List<ReportStructuredData.EquipmentRow> equipmentRows =
                queryRepository.findEquipmentRows(dateRange.startAt(), dateRange.endExclusive());

        return new ReportStructuredData(
                createSummaryRows(report, metrics, dateRange),
                lineRows.isEmpty()
                        ? List.of(new ReportStructuredData.LineRow(
                        MISSING_VALUE,
                        MISSING_VALUE,
                        MISSING_VALUE,
                        MISSING_VALUE,
                        MISSING_VALUE
                ))
                        : lineRows,
                equipmentRows.isEmpty()
                        ? List.of(new ReportStructuredData.EquipmentRow(
                        MISSING_VALUE,
                        MISSING_VALUE,
                        MISSING_VALUE,
                        MISSING_VALUE
                ))
                        : equipmentRows,
                createAnalysis(report)
        );
    }

    private List<ReportStructuredData.SummaryRow> createSummaryRows(
            Report report,
            ReportStructuredDataQueryRepository.SummaryMetrics metrics,
            DateRange dateRange
    ) {
        Map<String, String> values = createSummaryValueMap(report, metrics, dateRange);

        return List.of(
                row("보고서 기간", values.get("보고서 기간")),
                row("보고서 유형", values.get("보고서 유형")),
                row("총 주문 수", values.get("총 주문 수")),
                row("총 생산계획 수", values.get("총 생산계획 수")),
                row("총 생산 계획 수량", values.get("총 생산 계획 수량")),
                row("총 생산 완료 수량", values.get("총 생산 완료 수량")),
                row("생산 계획 대비 실적", values.get("생산 계획 대비 실적")),
                row("라인 가동률", values.get("라인 가동률")),
                row("평균 Cycle Time", values.get("평균 Cycle Time")),
                row("불량 수량", values.get("불량 수량")),
                row("불량률", values.get("불량률")),
                row("설비 다운 타임", values.get("설비 다운 타임")),
                row("작업자 투입 시간", values.get("작업자 투입 시간")),
                row("안전 사고 건수", values.get("안전 사고 건수")),
                row("납기 준수율", values.get("납기 준수율")),
                row("납기 위험 주문 수", values.get("납기 위험 주문 수")),
                row("자재 위험 품목 수", values.get("자재 위험 품목 수")),
                row("비정상 설비 상태 수", values.get("비정상 설비 상태 수"))
        );
    }

    private ReportStructuredData enrichStructuredData(Report report, ReportStructuredData structuredData) {
        DateRange dateRange = DateRange.from(
                report.getReportType(),
                report.getTargetStartDate(),
                report.getTargetEndDate()
        );
        ReportStructuredDataQueryRepository.SummaryMetrics metrics =
                queryRepository.findSummaryMetrics(
                        dateRange.startDate(),
                        dateRange.endDateExclusive(),
                        dateRange.startAt(),
                        dateRange.endExclusive()
                );
        List<ReportStructuredData.LineRow> lineRows =
                queryRepository.findLineRows(dateRange.startAt(), dateRange.endExclusive());
        Map<String, String> values = createSummaryValueMap(report, metrics, dateRange);

        return new ReportStructuredData(
                enrichSummaryRows(structuredData.summaryRows(), values),
                lineRows.isEmpty() ? structuredData.lineRows() : lineRows,
                structuredData.equipmentRows(),
                structuredData.analysis()
        );
    }

    private List<ReportStructuredData.SummaryRow> enrichSummaryRows(
            List<ReportStructuredData.SummaryRow> rows,
            Map<String, String> values
    ) {
        return rows.stream()
                .map(row -> {
                    String calculatedValue = values.get(row.label());
                    String value = shouldReplaceSummaryValue(row.label(), row.value(), calculatedValue)
                            ? calculatedValue
                            : row.value();
                    return new ReportStructuredData.SummaryRow(row.label(), value, row.change());
                })
                .toList();
    }

    private boolean shouldReplaceSummaryValue(String label, String currentValue, String calculatedValue) {
        if (!hasText(calculatedValue)) {
            return false;
        }

        if ("보고서 기간".equals(label) || "보고서 유형".equals(label)) {
            return true;
        }

        return !hasText(currentValue) || MISSING_VALUE.equals(currentValue.trim());
    }

    private Map<String, String> createSummaryValueMap(
            Report report,
            ReportStructuredDataQueryRepository.SummaryMetrics metrics,
            DateRange dateRange
    ) {
        BigDecimal plannedQuantity = defaultZero(metrics.plannedQuantity());
        BigDecimal actualQuantity = defaultZero(metrics.actualQuantity());
        BigDecimal defectQuantity = defaultZero(metrics.defectQuantity());
        BigDecimal actualDurationHours = defaultZero(metrics.actualDurationHours());

        Map<String, String> values = new LinkedHashMap<>();
        values.put("보고서 기간", formatPeriod(dateRange.startDate(), dateRange.endDate()));
        values.put("보고서 유형", reportTypeLabel(report.getReportType()));
        values.put("총 주문 수", formatNumber(metrics.orderCount()));
        values.put("총 생산계획 수", formatNumber(metrics.planCount()));
        values.put("총 생산 계획 수량", formatNumber(plannedQuantity));
        values.put("총 생산 완료 수량", formatNumber(actualQuantity));
        values.put("생산 계획 대비 실적", formatRate(actualQuantity, plannedQuantity));
        values.put("라인 가동률", formatPercent(metrics.avgLineUtilizationRate()));
        values.put("평균 Cycle Time", formatCycleTime(actualDurationHours, actualQuantity));
        values.put("불량 수량", formatNumber(defectQuantity));
        values.put("불량률", formatRate(defectQuantity, actualQuantity));
        values.put("설비 다운 타임", MISSING_VALUE);
        values.put("작업자 투입 시간", formatHours(actualDurationHours));
        values.put("안전 사고 건수", MISSING_VALUE);
        values.put("납기 준수율", formatOnTimeRate(metrics.onTimeOrderCount(), metrics.dueOrderCount()));
        values.put("납기 위험 주문 수", formatNumber(metrics.deliveryRiskOrderCount()));
        values.put("자재 위험 품목 수", formatNumber(metrics.materialRiskItemCount()));
        values.put("비정상 설비 상태 수", formatNumber(metrics.abnormalMachineCount()));
        return values;
    }

    private ReportStructuredData.SummaryRow row(String label, String value) {
        return new ReportStructuredData.SummaryRow(label, value, "-");
    }

    private ReportStructuredData.Analysis createAnalysis(Report report) {
        JsonNode sections = report.getReportContent() != null
                ? report.getReportContent().path("sections")
                : null;

        if (sections == null || !sections.isArray()) {
            return new ReportStructuredData.Analysis(
                    "분석 내용이 없습니다.",
                    List.of(),
                    "생성 필요"
            );
        }

        String overview = null;
        String recommendation = null;
        List<ReportStructuredData.AnalysisSection> analysisSections = new ArrayList<>();

        for (JsonNode section : sections) {
            String sectionKey = text(section, "section_key");
            String title = text(section, "section_title");
            String content = text(section, "content");

            if (!hasText(content)) {
                continue;
            }

            if ("summary".equals(sectionKey) && overview == null) {
                overview = content;
                continue;
            }

            if ("recommended_actions".equals(sectionKey)) {
                recommendation = content;
                continue;
            }

            analysisSections.add(new ReportStructuredData.AnalysisSection(
                    hasText(title) ? title : "분석",
                    splitItems(content)
            ));
        }

        if (!hasText(overview)) {
            overview = report.getReportTitle();
        }

        if (!hasText(recommendation)) {
            recommendation = "생성 필요";
        }

        return new ReportStructuredData.Analysis(overview, analysisSections, recommendation);
    }

    private List<String> splitItems(String content) {
        String[] lines = content.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        List<String> items = new ArrayList<>();

        for (String line : lines) {
            String item = normalizeMarkdownLine(line);
            if (hasText(item)) {
                items.add(item);
            }
        }

        return items.isEmpty() ? List.of(content) : items;
    }

    private String normalizeMarkdownLine(String line) {
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

    private boolean hasStructuredData(Report report) {
        return hasStructuredFields(report.getIncludedItems())
                || hasStructuredFields(field(report.getIncludedItems(), "sections"))
                || hasStructuredFields(report.getReportContent())
                || hasStructuredFields(field(report.getReportContent(), "sections"));
    }

    private boolean hasStructuredFields(JsonNode node) {
        return node != null
                && node.isObject()
                && (node.has("summaryRows")
                || node.has("summary_rows")
                || node.has("lineRows")
                || node.has("line_rows")
                || node.has("equipmentRows")
                || node.has("equipment_rows")
                || node.has("analysis"));
    }

    private JsonNode field(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            return null;
        }

        return node.get(fieldName);
    }

    private String extractMarkdown(JsonNode reportContent) {
        if (reportContent == null || reportContent.isNull()) {
            return null;
        }

        JsonNode markdownNode = reportContent.path("markdown");
        if (!markdownNode.isMissingNode() && !markdownNode.isNull()) {
            return markdownNode.asText();
        }

        JsonNode sections = reportContent.path("sections");
        if (!sections.isArray()) {
            return reportContent.toString();
        }

        StringBuilder markdownBuilder = new StringBuilder();
        for (JsonNode section : sections) {
            String title = text(section, "section_title");
            String content = text(section, "content");

            if (!hasText(content)) {
                continue;
            }

            if (!markdownBuilder.isEmpty()) {
                markdownBuilder.append("\n\n");
            }

            if (hasText(title)) {
                markdownBuilder.append("## ").append(title).append("\n");
            }
            markdownBuilder.append(content);
        }

        return !markdownBuilder.isEmpty() ? markdownBuilder.toString() : reportContent.toString();
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || !node.isObject()) {
            return null;
        }

        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        return value.asText();
    }

    private String formatPeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return MISSING_VALUE;
        }

        if (startDate == null) {
            return DISPLAY_DATE_FORMATTER.format(endDate);
        }

        if (endDate == null) {
            return DISPLAY_DATE_FORMATTER.format(startDate);
        }

        return DISPLAY_DATE_FORMATTER.format(startDate) + " ~ " + DISPLAY_DATE_FORMATTER.format(endDate);
    }

    private String reportTypeLabel(ReportType reportType) {
        if (reportType == null) {
            return MISSING_VALUE;
        }

        return switch (reportType) {
            case MONTHLY, MONTHLY_BUSINESS -> "월간";
            case ON_DEMAND, ON_DEMAND_BUSINESS -> "수시";
        };
    }

    private String formatRate(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return "0%";
        }

        return numerator
                .multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "%";
    }

    private String formatOnTimeRate(long onTimeOrderCount, long dueOrderCount) {
        if (dueOrderCount <= 0) {
            return MISSING_VALUE;
        }

        return formatRate(BigDecimal.valueOf(onTimeOrderCount), BigDecimal.valueOf(dueOrderCount));
    }

    private String formatPercent(BigDecimal rate) {
        if (rate == null) {
            return MISSING_VALUE;
        }

        return rate
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "%";
    }

    private String formatCycleTime(BigDecimal actualDurationHours, BigDecimal actualQuantity) {
        if (actualQuantity == null || actualQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return MISSING_VALUE;
        }

        return actualDurationHours
                .divide(actualQuantity, 4, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "시간/개";
    }

    private String formatHours(BigDecimal hours) {
        if (hours == null) {
            return MISSING_VALUE;
        }

        return hours
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "시간";
    }

    private String formatNumber(long value) {
        return String.valueOf(value);
    }

    private String formatNumber(BigDecimal value) {
        if (value == null) {
            return MISSING_VALUE;
        }

        return value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record DateRange(
            LocalDate startDate,
            LocalDate endDate,
            LocalDate endDateExclusive,
            OffsetDateTime startAt,
            OffsetDateTime endExclusive
    ) {
        private static DateRange from(ReportType reportType, LocalDate startDate, LocalDate endDate) {
            ResolvedPeriod period = ReportPeriodSupport.resolve(reportType, startDate, endDate);
            LocalDate endDateExclusive = period.endDateExclusive();
            OffsetDateTime startAt = period.startDate()
                    .atStartOfDay(DEFAULT_PRODUCTION_ZONE)
                    .toOffsetDateTime();
            OffsetDateTime endExclusive = endDateExclusive
                    .atStartOfDay(DEFAULT_PRODUCTION_ZONE)
                    .toOffsetDateTime();

            return new DateRange(period.startDate(), period.endDate(), endDateExclusive, startAt, endExclusive);
        }
    }
}
