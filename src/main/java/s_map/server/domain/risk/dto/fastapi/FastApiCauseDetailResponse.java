package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiCauseDetailResponse(

        @JsonProperty("raw_delay_probability")
        @JsonAlias("rawDelayProbability")
        BigDecimal rawDelayProbability,

        @JsonProperty("calibrated_delay_probability")
        @JsonAlias("calibratedDelayProbability")
        BigDecimal calibratedDelayProbability,

        @JsonProperty("probability_output")
        @JsonAlias("probabilityOutput")
        String probabilityOutput,

        @JsonProperty("top_factors")
        @JsonAlias("topFactors")
        List<FastApiShapFactorResponse> topFactors,

        @JsonProperty("risk_increase_factors")
        @JsonAlias("riskIncreaseFactors")
        List<FastApiShapFactorResponse> riskIncreaseFactors,

        @JsonProperty("risk_decrease_factors")
        @JsonAlias("riskDecreaseFactors")
        List<FastApiShapFactorResponse> riskDecreaseFactors
) {
}
