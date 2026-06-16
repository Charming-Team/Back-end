package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "FastAPI 지연 시간 예측 요청")
public record FastApiDelayPredictionRequest(

        @JsonProperty("orderId")
        @Schema(description = "예측 대상 주문 ID", example = "431")
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
