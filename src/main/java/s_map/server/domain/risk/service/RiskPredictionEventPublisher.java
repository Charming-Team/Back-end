package s_map.server.domain.risk.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import s_map.server.domain.risk.entity.RiskPredictionTriggerType;

import java.util.Objects;

@Component
public class RiskPredictionEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public RiskPredictionEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishOrderCreated(Long orderId) {
        publish(orderId, RiskPredictionTriggerType.ORDER_CREATED);
    }

    public void publishOrderUpdated(Long orderId) {
        publish(orderId, RiskPredictionTriggerType.ORDER_UPDATED);
    }

    public void publishPlanChanged(Long orderId) {
        publish(orderId, RiskPredictionTriggerType.PLAN_CHANGED);
    }

    public void publish(
            Long orderId,
            RiskPredictionTriggerType triggerType
    ) {
        validateOrderId(orderId);
        Objects.requireNonNull(triggerType, "triggerType must not be null");

        eventPublisher.publishEvent(
                RiskPredictionTriggerEvent.of(orderId, triggerType)
        );
    }

    private static void validateOrderId(Long orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");

        if (orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive.");
        }
    }
}