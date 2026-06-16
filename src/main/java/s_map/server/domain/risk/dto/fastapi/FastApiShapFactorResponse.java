package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "FastAPI SHAP 요인 응답")
@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiShapFactorResponse(

        @JsonProperty("feature")
        @Schema(description = "모델 feature 이름", example = "due_margin_to_duration_ratio_capped")
        String feature,

        @JsonProperty("feature_name_ko")
        @Schema(description = "feature 한글명", example = "생산 소요시간 대비 납기 여유 비율")
        String featureNameKo,

        @JsonProperty("cause_tag")
        @Schema(description = "원인 태그", example = "DUE_MARGIN_RISK")
        String causeTag,

        @JsonProperty("feature_value")
        @Schema(description = "feature 값", example = "0.8")
        Object featureValue,

        @JsonProperty("impact")
        @Schema(description = "SHAP 영향도", example = "0.3125")
        BigDecimal impact,

        @JsonProperty("abs_impact")
        @Schema(description = "SHAP 영향도 절댓값", example = "0.3125")
        BigDecimal absImpact,

        @JsonProperty("direction")
        @Schema(description = "영향 방향", example = "increase")
        String direction
) {
}
