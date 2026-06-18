package s_map.server.domain.risk.service;

import s_map.server.domain.risk.entity.RiskLevel;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

public record RiskAgentAnalysisTriggerEvent(
        Long predictionId,
        Long orderId,
        RiskLevel riskLevel,
        BigDecimal delayProbability,
        BigDecimal predictedDelayDays,
        OffsetDateTime triggeredAt
) {

    public RiskAgentAnalysisTriggerEvent {
        Objects.requireNonNull(predictionId, "predictionId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(riskLevel, "riskLevel must not be null");
        Objects.requireNonNull(delayProbability, "delayProbability must not be null");
        Objects.requireNonNull(triggeredAt, "triggeredAt must not be null");
    }
}