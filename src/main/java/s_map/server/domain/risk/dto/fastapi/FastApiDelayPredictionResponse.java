package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiDelayPredictionResponse(

        @JsonProperty("orderId")
        Long orderId,

        @JsonProperty("predictedDelayHours")
        BigDecimal predictedDelayHours,

        @JsonProperty("modelName")
        String modelName
) {
}