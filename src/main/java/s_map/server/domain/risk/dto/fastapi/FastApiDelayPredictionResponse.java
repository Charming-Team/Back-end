package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "FastAPI 지연 시간 예측 응답")
@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiDelayPredictionResponse(

        @JsonProperty("orderId")
        @Schema(description = "주문 ID", example = "431")
        Long orderId,

        @JsonProperty("predictedDelayHours")
        @Schema(description = "예상 지연 시간", example = "12.50")
        BigDecimal predictedDelayHours,

        @JsonProperty("modelName")
        @Schema(description = "모델명", example = "xgboost_delay_hours")
        String modelName
) {
}
