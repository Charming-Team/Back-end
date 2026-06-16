package s_map.server.domain.risk.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "리스크 원인 요인 응답")
public record RiskCauseResponse(
        @Schema(description = "원인 태그 코드", example = "DUE_MARGIN_RISK")
        String causeType,

        @Schema(description = "원인 태그 한글 표시명", example = "납기 여유 부족")
        String causeTypeLabel,

        @Schema(description = "원인 요인 제목", example = "생산 소요시간 대비 납기 여유 비율")
        String title,

        @Schema(description = "원인 요인 설명", example = "생산 소요시간 대비 납기 여유 비율 값은 0.8이며, 지연 위험을 증가시키는 방향으로 작용했습니다.")
        String description,

        @Schema(description = "원인 판단 근거", nullable = true, example = "납기 여유가 기준치보다 낮습니다.")
        String evidence,

        @Schema(description = "SHAP 영향도 값", example = "0.3125")
        BigDecimal impact,

        @Schema(description = "지연 위험 영향 방향", example = "increase")
        String direction
) {
}
