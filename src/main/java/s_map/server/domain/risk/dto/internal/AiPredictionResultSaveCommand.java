package s_map.server.domain.risk.dto.internal;

import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

public record AiPredictionResultSaveCommand(
        Long orderId,
        Long productId,
        Long planId,
        Long lineId,
        BigDecimal delayProbability,
        BigDecimal predictedDelayDays,
        RiskLevel riskLevel,
        String modelName,
        String modelVersion,
        OffsetDateTime predictedAt,
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