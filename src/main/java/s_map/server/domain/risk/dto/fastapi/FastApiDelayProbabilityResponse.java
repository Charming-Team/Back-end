package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import s_map.server.domain.risk.dto.internal.AiPredictionResultSaveCommand;
import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiDelayProbabilityResponse(

        @JsonProperty("order_id")
        Long orderId,

        @JsonProperty("product_id")
        Long productId,

        @JsonProperty("plan_id")
        Long planId,

        @JsonProperty("line_id")
        Long lineId,

        @JsonProperty("raw_delay_probability")
        BigDecimal rawDelayProbability,

        @JsonProperty("delay_probability")
        BigDecimal delayProbability,

        @JsonProperty("risk_level")
        RiskLevel riskLevel,

        @JsonProperty("model_name")
        String modelName,

        @JsonProperty("model_version")
        String modelVersion,

        @JsonProperty("probability_output")
        String probabilityOutput,

        @JsonProperty("predicted_at")
        OffsetDateTime predictedAt,

        @JsonProperty("top_factors")
        List<FastApiShapFactorResponse> topFactors,

        @JsonProperty("risk_increase_factors")
        List<FastApiShapFactorResponse> riskIncreaseFactors,

        @JsonProperty("risk_decrease_factors")
        List<FastApiShapFactorResponse> riskDecreaseFactors,

        @JsonProperty("cause_detail")
        FastApiCauseDetailResponse causeDetail
) {

    public AiPredictionResultSaveCommand toSaveCommand(ObjectMapper objectMapper) {
        Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        validateRequiredFields();

        JsonNode causeDetailJson = objectMapper.valueToTree(causeDetail);

        return new AiPredictionResultSaveCommand(
                orderId,
                productId,
                planId,
                lineId,
                delayProbability,
                riskLevel,
                modelName,
                modelVersion,
                predictedAt,
                causeDetailJson
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