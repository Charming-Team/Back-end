package s_map.server.domain.report.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.support.ReportPeriodSupport;
import s_map.server.domain.report.support.ReportPeriodSupport.ResolvedPeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "보고서 목록 조회 응답")
public class ReportListResponse {

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

    @Schema(description = "보고서 생성 일시", example = "2026-06-03T10:00:00")
    private LocalDateTime createdAt;

    public static ReportListResponse from(Report report, String authorName) {
        ResolvedPeriod period = ReportPeriodSupport.resolve(
                report.getReportType(),
                report.getTargetStartDate(),
                report.getTargetEndDate()
        );

        return ReportListResponse.builder()
                .reportId(report.getReportId())
                .title(report.getReportTitle())
                .reportType(report.getReportType().name())
                .authorId(report.getAuthorId())
                .authorName(authorName)
                .targetStartDate(period.startDate())
                .targetEndDate(period.endDate())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
