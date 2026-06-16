package s_map.server.domain.risk.dto.internal;

import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

public record AiPredictionResultSaveResult(
        Long predictionId,
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
        boolean requiresAgentAnalysis
) {

    public AiPredictionResultSaveResult {
        Objects.requireNonNull(predictionId, "predictionId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(productId, "productId must not be null");
        Objects.requireNonNull(delayProbability, "delayProbability must not be null");
        Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        Objects.requireNonNull(modelName, "modelName must not be null");
        Objects.requireNonNull(modelVersion, "modelVersion must not be null");
        Objects.requireNonNull(predictedAt, "predictedAt must not be null");
    }

    public static AiPredictionResultSaveResult from(
            Long predictionId,
            AiPredictionResultSaveCommand command
    ) {
        Objects.requireNonNull(command, "command must not be null");

        return new AiPredictionResultSaveResult(
                predictionId,
                command.orderId(),
                command.productId(),
                command.planId(),
                command.lineId(),
                command.delayProbability(),
                command.predictedDelayDays(),
                command.riskLevel(),
                command.modelName(),
                command.modelVersion(),
                command.predictedAt(),
                command.requiresAgentAnalysis()
        );
    }
}