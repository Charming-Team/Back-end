package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import s_map.server.domain.risk.dto.internal.AiPredictionResultSaveCommand;
import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiDelayProbabilityResponse(

        @JsonProperty("order_id")
        @JsonAlias("orderId")
        Long orderId,

        @JsonProperty("product_id")
        @JsonAlias("productId")
        Long productId,

        @JsonProperty("plan_id")
        @JsonAlias("planId")
        Long planId,

        @JsonProperty("line_id")
        @JsonAlias("lineId")
        Long lineId,

        @JsonProperty("raw_delay_probability")
        @JsonAlias("rawDelayProbability")
        BigDecimal rawDelayProbability,

        @JsonProperty("delay_probability")
        @JsonAlias("delayProbability")
        BigDecimal delayProbability,

        @JsonProperty("risk_level")
        @JsonAlias("riskLevel")
        RiskLevel riskLevel,

        @JsonProperty("model_name")
        @JsonAlias("modelName")
        String modelName,

        @JsonProperty("model_version")
        @JsonAlias("modelVersion")
        String modelVersion,

        @JsonProperty("probability_output")
        @JsonAlias("probabilityOutput")
        String probabilityOutput,

        @JsonProperty("predicted_at")
        @JsonAlias("predictedAt")
        OffsetDateTime predictedAt,

        @JsonProperty("top_factors")
        @JsonAlias("topFactors")
        List<FastApiShapFactorResponse> topFactors,

        @JsonProperty("risk_increase_factors")
        @JsonAlias("riskIncreaseFactors")
        List<FastApiShapFactorResponse> riskIncreaseFactors,

        @JsonProperty("risk_decrease_factors")
        @JsonAlias("riskDecreaseFactors")
        List<FastApiShapFactorResponse> riskDecreaseFactors,

        @JsonProperty("cause_detail")
        @JsonAlias("causeDetail")
        FastApiCauseDetailResponse causeDetail
) {

    public AiPredictionResultSaveCommand toSaveCommand(
            BigDecimal predictedDelayDays
    ) {
        validateRequiredFields();

        return new AiPredictionResultSaveCommand(
                orderId,
                productId,
                planId,
                lineId,
                delayProbability,
                predictedDelayDays,
                riskLevel,
                modelName,
                modelVersion,
                predictedAt,
                causeDetail
        );
    }

    private void validateRequiredFields() {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(delayProbability, "delayProbability must not be null");
        Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        Objects.requireNonNull(modelName, "modelName must not be null");
        Objects.requireNonNull(modelVersion, "modelVersion must not be null");
        Objects.requireNonNull(predictedAt, "predictedAt must not be null");
        Objects.requireNonNull(causeDetail, "causeDetail must not be null");
    }
}
