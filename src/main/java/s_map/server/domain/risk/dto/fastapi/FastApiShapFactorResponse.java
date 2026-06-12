package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiShapFactorResponse(

        @JsonProperty("feature")
        String feature,

        @JsonProperty("feature_name_ko")
        String featureNameKo,

        @JsonProperty("cause_tag")
        String causeTag,

        @JsonProperty("feature_value")
        JsonNode featureValue,

        @JsonProperty("impact")
        BigDecimal impact,

        @JsonProperty("abs_impact")
        BigDecimal absImpact,

        @JsonProperty("direction")
        String direction
) {
}