package s_map.server.domain.risk.service;

import s_map.server.domain.risk.entity.RiskPredictionTriggerType;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

public record RiskPredictionTriggerEvent(
        Long orderId,
        RiskPredictionTriggerType triggerType,
        OffsetDateTime triggeredAt
) {

    public RiskPredictionTriggerEvent {
        Objects.requireNonNull(orderId, "orderId must not be null");
        Objects.requireNonNull(triggerType, "triggerType must not be null");
        Objects.requireNonNull(triggeredAt, "triggeredAt must not be null");

        if (orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive.");
        }
    }

    public static RiskPredictionTriggerEvent of(
            Long orderId,
            RiskPredictionTriggerType triggerType
    ) {
        return new RiskPredictionTriggerEvent(
                orderId,
                triggerType,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}