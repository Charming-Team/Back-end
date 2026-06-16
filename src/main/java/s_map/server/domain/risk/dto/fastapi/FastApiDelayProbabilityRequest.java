package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "FastAPI 지연 확률 예측 요청")
public record FastApiDelayProbabilityRequest(

        @JsonProperty("orderId")
        @Schema(description = "예측 대상 주문 ID", example = "431")
        Long orderId,

        @JsonProperty("topN")
        @Schema(description = "응답에 포함할 상위 요인 개수", example = "5")
        Integer topN
) {

    private static final int DEFAULT_TOP_N = 5;

    public FastApiDelayProbabilityRequest {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive.");
        }

        if (topN == null) {
            topN = DEFAULT_TOP_N;
        }

        if (topN <= 0 || topN > 20) {
            throw new IllegalArgumentException("topN must be between 1 and 20.");
        }
    }

    public static FastApiDelayProbabilityRequest of(Long orderId) {
        return new FastApiDelayProbabilityRequest(orderId, DEFAULT_TOP_N);
    }

    public static FastApiDelayProbabilityRequest of(Long orderId, Integer topN) {
        return new FastApiDelayProbabilityRequest(orderId, topN);
    }
}
