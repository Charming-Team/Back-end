package s_map.server.domain.report.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import s_map.server.domain.report.dto.res.ReportStructuredData;

import java.util.List;

@Schema(description = "보고서 내용 수정 요청")
public record ReportUpdateRequest(

        @Schema(description = "수정할 보고서 제목", example = "2026년 6월 월간 생산계획 운영 보고서")
        @NotBlank(message = "보고서 제목은 필수입니다.")
        @Size(max = 200, message = "보고서 제목은 200자 이하여야 합니다.")
        String title,

        @Schema(description = "수정할 보고서 Markdown 본문")
        String markdown,

        @Schema(description = "주요 요약 행")
        List<ReportStructuredData.SummaryRow> summaryRows,

        @Schema(description = "라인별 성과 행")
        List<ReportStructuredData.LineRow> lineRows,

        @Schema(description = "주요 설비 현황 행")
        List<ReportStructuredData.EquipmentRow> equipmentRows,

        @Schema(description = "보고서 요약 및 분석")
        @Valid
        ReportStructuredData.Analysis analysis
) {
}
