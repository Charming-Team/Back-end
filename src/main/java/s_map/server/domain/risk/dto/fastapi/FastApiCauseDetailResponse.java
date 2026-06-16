package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiCauseDetailResponse(

        @JsonProperty("raw_delay_probability")
        BigDecimal rawDelayProbability,

        @JsonProperty("calibrated_delay_probability")
        BigDecimal calibratedDelayProbability,

        @JsonProperty("probability_output")
        String probabilityOutput,

        @JsonProperty("top_factors")
        List<FastApiShapFactorResponse> topFactors,

        @JsonProperty("risk_increase_factors")
        List<FastApiShapFactorResponse> riskIncreaseFactors,

        @JsonProperty("risk_decrease_factors")
        List<FastApiShapFactorResponse> riskDecreaseFactors
) {
}