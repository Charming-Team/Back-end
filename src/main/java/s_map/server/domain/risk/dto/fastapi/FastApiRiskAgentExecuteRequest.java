package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.Objects;

public record FastApiRiskAgentExecuteRequest(

        @JsonProperty("predictionId")
        Long predictionId,

        @JsonProperty("orderId")
        Long orderId,

        @JsonProperty("triggeredAt")
        OffsetDateTime triggeredAt
) {

    public FastApiRiskAgentExecuteRequest {
        Objects.requireNonNull(
                predictionId,
                "predictionId must not be null"
        );
        Objects.requireNonNull(
                orderId,
                "orderId must not be null"
        );
        Objects.requireNonNull(
                triggeredAt,
                "triggeredAt must not be null"
        );

        if (predictionId <= 0) {
            throw new IllegalArgumentException(
                    "predictionId must be positive."
            );
        }

        if (orderId <= 0) {
            throw new IllegalArgumentException(
                    "orderId must be positive."
            );
        }
    }
}