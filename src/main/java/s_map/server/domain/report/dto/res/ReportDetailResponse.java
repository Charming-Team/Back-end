package s_map.server.domain.report.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.support.ReportPeriodSupport;
import s_map.server.domain.report.support.ReportPeriodSupport.ResolvedPeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@Schema(description = "보고서 상세 조회 응답")
public class ReportDetailResponse {

    @Schema(description = "보고서 ID", example = "1")
    private Long reportId;

    @Schema(description = "보고서 제목", example = "2026년 5월 월간 생산 보고서")
    private String title;

    @Schema(description = "보고서 유형", example = "MONTHLY")
    private String reportType;

    @Schema(description = "작성자 사용자 ID", example = "1")
    private Long authorId;

    @Schema(description = "작성자 이름", example = "관리자")
    private String authorName;

    @Schema(description = "보고서 대상 시작일", example = "2026-05-01")
    private LocalDate targetStartDate;

    @Schema(description = "보고서 대상 종료일", example = "2026-05-31")
    private LocalDate targetEndDate;

    @Schema(description = "보고서 상세 화면 섹션 데이터")
    private Object sections;

    @Schema(description = "주요 요약 표 행 목록")
    private List<ReportStructuredData.SummaryRow> summaryRows;

    @Schema(description = "라인별 성과 표 행 목록")
    private List<ReportStructuredData.LineRow> lineRows;

    @Schema(description = "주요 설비 현황 표 행 목록")
    private List<ReportStructuredData.EquipmentRow> equipmentRows;

    @Schema(description = "보고서 요약 및 분석 데이터")
    private ReportStructuredData.Analysis analysis;

    @Schema(description = "보고서 생성 근거 데이터")
    private Object evidence;

    @Schema(description = "보고서 본문 Markdown", example = "## 주요 요약\\n- 총 생산량이 계획 대비 98%를 달성했습니다.")
    private String markdown;

    @Schema(description = "연결된 시뮬레이션 ID", example = "1001", nullable = true)
    private Long relatedSimulationId;

    @Schema(description = "경영진용 보고서 생성 기준 원본 보고서 ID", example = "10", nullable = true)
    private Long sourceReportId;

    @Schema(description = "경영진용 보고서 생성 기준 원본 보고서 제목", example = "2026년 6월 수시 보고서", nullable = true)
    private String sourceReportTitle;

    @Schema(description = "경영진용 보고서 생성 기준 원본 보고서 유형", example = "ON_DEMAND", nullable = true)
    private String sourceReportType;

    @Schema(description = "보고서 생성 일시", example = "2026-06-03T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "보고서 수정 일시", example = "2026-06-03T10:05:00")
    private LocalDateTime updatedAt;

    public static ReportDetailResponse from(Report report, String authorName) {
        String markdown = extractMarkdown(report.getReportContent());
        ReportStructuredData structuredData = ReportStructuredData.from(report, markdown);

        return from(report, authorName, structuredData);
    }

    public static ReportDetailResponse from(
            Report report,
            String authorName,
            ReportStructuredData structuredData
    ) {
        String markdown = extractMarkdown(report.getReportContent());
        ResolvedPeriod period = ReportPeriodSupport.resolve(
                report.getReportType(),
                report.getTargetStartDate(),
                report.getTargetEndDate()
        );

        return ReportDetailResponse.builder()
                .reportId(report.getReportId())
                .title(report.getReportTitle())
                .reportType(report.getReportType().name())
                .authorId(report.getAuthorId())
                .authorName(authorName)
                .targetStartDate(period.startDate())
                .targetEndDate(period.endDate())
                .sections(toJsonValue(report.getIncludedItems()))
                .summaryRows(structuredData.summaryRows())
                .lineRows(structuredData.lineRows())
                .equipmentRows(structuredData.equipmentRows())
                .analysis(structuredData.analysis())
                .evidence(toJsonValue(report.getReportEvidence()))
                .markdown(markdown)
                .relatedSimulationId(report.getRelatedSimulationId())
                .sourceReportId(extractLong(report.getReportContent(), "source_report_id"))
                .sourceReportTitle(extractText(report.getReportContent(), "source_report_title"))
                .sourceReportType(extractText(report.getReportContent(), "source_report_type"))
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private static String extractMarkdown(JsonNode reportContent) {
        if (reportContent == null || reportContent.isNull()) {
            return null;
        }

        JsonNode markdownNode = reportContent.path("markdown");

        if (!markdownNode.isMissingNode() && !markdownNode.isNull()) {
            return markdownNode.asText();
        }

        JsonNode sectionsNode = reportContent.path("sections");

        if (sectionsNode.isArray()) {
            StringBuilder markdownBuilder = new StringBuilder();

            for (JsonNode sectionNode : sectionsNode) {
                JsonNode contentNode = sectionNode.path("content");

                if (!contentNode.isMissingNode() && !contentNode.isNull()) {
                    if (!markdownBuilder.isEmpty()) {
                        markdownBuilder.append("\n\n");
                    }

                    markdownBuilder.append(contentNode.asText());
                }
            }

            if (!markdownBuilder.isEmpty()) {
                return markdownBuilder.toString();
            }
        }

        return reportContent.toString();
    }

    private static Long extractLong(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }

        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || !value.canConvertToLong()) {
            return null;
        }

        return value.asLong();
    }

    private static String extractText(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }

        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        return value.asText();
    }

    private static Object toJsonValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                map.put(field.getKey(), toJsonValue(field.getValue()));
            }

            return map;
        }

        if (node.isArray()) {
            List<Object> list = new ArrayList<>();

            for (JsonNode item : node) {
                list.add(toJsonValue(item));
            }

            return list;
        }

        if (node.isTextual()) {
            return node.asText();
        }

        if (node.isBoolean()) {
            return node.asBoolean();
        }

        if (node.isInt()) {
            return node.asInt();
        }

        if (node.isLong()) {
            return node.asLong();
        }

        if (node.isDouble() || node.isFloat() || node.isBigDecimal()) {
            return node.asDouble();
        }

        if (node.isNumber()) {
            return node.numberValue();
        }

        return node.asText();
    }
}
