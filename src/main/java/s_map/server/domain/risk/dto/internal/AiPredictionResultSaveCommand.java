package s_map.server.domain.risk.dto.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Schema(description = "AI 예측 결과 저장 명령")
public record AiPredictionResultSaveCommand(
        @Schema(description = "주문 고유 ID", example = "101")
        Long orderId,

        @Schema(description = "제품 고유 ID", example = "12")
        Long productId,

        @Schema(description = "생산계획 고유 ID", example = "3001", nullable = true)
        Long planId,

        @Schema(description = "생산 라인 고유 ID", example = "5", nullable = true)
        Long lineId,

        @Schema(description = "지연 확률(0~1)", example = "0.73")
        BigDecimal delayProbability,

        @Schema(description = "예상 지연 일수", example = "2.5", nullable = true)
        BigDecimal predictedDelayDays,

        @Schema(description = "위험 등급", example = "HIGH")
        RiskLevel riskLevel,

        @Schema(description = "예측 모델명", example = "delay-probability")
        String modelName,

        @Schema(description = "예측 모델 버전", example = "v1.0.0")
        String modelVersion,

        @Schema(description = "예측 수행 일시", example = "2026-06-16T09:00:00+09:00")
        OffsetDateTime predictedAt,

        @Schema(description = "예측 원인 상세 데이터")
        Object causeDetail
) {

    public AiPredictionResultSaveCommand {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(delayProbability, "delayProbability must not be null");
        Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        Objects.requireNonNull(modelName, "modelName must not be null");
        Objects.requireNonNull(modelVersion, "modelVersion must not be null");
        Objects.requireNonNull(predictedAt, "predictedAt must not be null");
        Objects.requireNonNull(causeDetail, "causeDetail must not be null");

        if (delayProbability.compareTo(BigDecimal.ZERO) < 0
                || delayProbability.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("delayProbability must be between 0 and 1.");
        }

        if (modelName.isBlank()) {
            throw new IllegalArgumentException("modelName must not be blank.");
        }

        if (modelVersion.isBlank()) {
            throw new IllegalArgumentException("modelVersion must not be blank.");
        }
    }

    public boolean requiresAgentAnalysis() {
        return riskLevel != RiskLevel.SAFE;
    }
}
