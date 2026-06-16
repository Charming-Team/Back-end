package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "FastAPI 지연 확률 원인 상세 응답")
@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiCauseDetailResponse(

        @JsonProperty("raw_delay_probability")
        @Schema(description = "보정 전 지연 확률", example = "0.5123")
        BigDecimal rawDelayProbability,

        @JsonProperty("calibrated_delay_probability")
        @Schema(description = "보정 후 지연 확률", example = "0.6250")
        BigDecimal calibratedDelayProbability,

        @JsonProperty("probability_output")
        @Schema(description = "확률 출력 방식", example = "calibrated_sigmoid")
        String probabilityOutput,

        @JsonProperty("top_factors")
        @Schema(description = "상위 SHAP 요인 목록")
        List<FastApiShapFactorResponse> topFactors,

        @JsonProperty("risk_increase_factors")
        @Schema(description = "지연 위험 증가 요인 목록")
        List<FastApiShapFactorResponse> riskIncreaseFactors,

        @JsonProperty("risk_decrease_factors")
        @Schema(description = "지연 위험 감소 요인 목록")
        List<FastApiShapFactorResponse> riskDecreaseFactors
) {
}
