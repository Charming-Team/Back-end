package s_map.server.domain.report.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.entity.Report;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Schema(description = "보고서 생성 근거 데이터")
    private Object evidence;

    @Schema(description = "보고서 본문 Markdown", example = "## 주요 요약\\n- 총 생산량이 계획 대비 98%를 달성했습니다.")
    private String markdown;

    @Schema(description = "연결된 시뮬레이션 ID", example = "1001", nullable = true)
    private Long relatedSimulationId;

    @Schema(description = "보고서 생성 일시", example = "2026-06-03T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "보고서 수정 일시", example = "2026-06-03T10:05:00")
    private LocalDateTime updatedAt;

    private static String extractMarkdown(JsonNode reportContent) {
        if (reportContent == null || reportContent.isNull()) {
            return null;
        }

        JsonNode markdownNode = reportContent.path("markdown");

        if (markdownNode.isMissingNode() || markdownNode.isNull()) {
            return reportContent.toString();
        }

        return markdownNode.asText();
    }

    public static ReportDetailResponse from(Report report, String authorName) {
        return ReportDetailResponse.builder()
                .reportId(report.getReportId())
                .title(report.getReportTitle())
                .reportType(report.getReportType().name())
                .authorId(report.getAuthorId())
                .authorName(authorName)
                .targetStartDate(report.getTargetStartDate())
                .targetEndDate(report.getTargetEndDate())
                .sections(report.getIncludedItems())
                .evidence(report.getReportEvidence())
                .markdown(extractMarkdown(report.getReportContent()))
                .relatedSimulationId(report.getRelatedSimulationId())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
