package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FastApiDelayPredictionRequest(

        @JsonProperty("orderId")
        Long orderId
) {

    public FastApiDelayPredictionRequest {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive.");
        }
    }

    public static FastApiDelayPredictionRequest of(Long orderId) {
        return new FastApiDelayPredictionRequest(orderId);
    }
}