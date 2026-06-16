package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.risk.dto.internal.AiPredictionResultSaveCommand;
import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Schema(description = "FastAPI 지연 확률 예측 응답")
@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiDelayProbabilityResponse(

        @JsonProperty("order_id")
        @Schema(description = "주문 ID", example = "431")
        Long orderId,

        @JsonProperty("product_id")
        @Schema(description = "제품 ID", example = "3")
        Long productId,

        @JsonProperty("plan_id")
        @Schema(description = "생산계획 ID", nullable = true, example = "431")
        Long planId,

        @JsonProperty("line_id")
        @Schema(description = "생산 라인 ID", nullable = true, example = "5")
        Long lineId,

        @JsonProperty("raw_delay_probability")
        @Schema(description = "보정 전 지연 확률", example = "0.5123")
        BigDecimal rawDelayProbability,

        @JsonProperty("delay_probability")
        @Schema(description = "보정 후 지연 확률", example = "0.6250")
        BigDecimal delayProbability,

        @JsonProperty("risk_level")
        @Schema(description = "위험도 코드", example = "WARNING")
        RiskLevel riskLevel,

        @JsonProperty("model_name")
        @Schema(description = "모델명", example = "xgboost_delay_probability")
        String modelName,

        @JsonProperty("model_version")
        @Schema(description = "모델 버전", example = "v1.0.0")
        String modelVersion,

        @JsonProperty("probability_output")
        @Schema(description = "확률 출력 방식", example = "calibrated_sigmoid")
        String probabilityOutput,

        @JsonProperty("predicted_at")
        @Schema(description = "예측 생성 시각", example = "2026-06-11T09:22:30+09:00")
        OffsetDateTime predictedAt,

        @JsonProperty("top_factors")
        @Schema(description = "상위 SHAP 요인 목록")
        List<FastApiShapFactorResponse> topFactors,

        @JsonProperty("risk_increase_factors")
        @Schema(description = "지연 위험 증가 요인 목록")
        List<FastApiShapFactorResponse> riskIncreaseFactors,

        @JsonProperty("risk_decrease_factors")
        @Schema(description = "지연 위험 감소 요인 목록")
        List<FastApiShapFactorResponse> riskDecreaseFactors,

        @JsonProperty("cause_detail")
        @Schema(description = "원인 상세 payload")
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
