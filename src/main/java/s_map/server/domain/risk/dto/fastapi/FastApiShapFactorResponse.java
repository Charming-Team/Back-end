package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiShapFactorResponse(

        @JsonProperty("feature")
        String feature,

        @JsonProperty("feature_name_ko")
        @JsonAlias("featureNameKo")
        String featureNameKo,

        @JsonProperty("cause_tag")
        @JsonAlias("causeTag")
        String causeTag,

        @JsonProperty("feature_value")
        @JsonAlias("featureValue")
        Object featureValue,

        @JsonProperty("impact")
        BigDecimal impact,

        @JsonProperty("abs_impact")
        @JsonAlias("absImpact")
        BigDecimal absImpact,

        @JsonProperty("direction")
        String direction
) {
}
