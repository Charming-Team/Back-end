package s_map.server.domain.risk.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import s_map.server.domain.risk.dto.internal.AiPredictionResultSaveResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class RiskAgentAnalysisEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public RiskAgentAnalysisEventPublisher(
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishIfRequired(
            AiPredictionResultSaveResult result
    ) {
        if (result == null || !result.requiresAgentAnalysis()) {
            return;
        }

        applicationEventPublisher.publishEvent(
                new RiskAgentAnalysisTriggerEvent(
                        result.predictionId(),
                        result.orderId(),
                        result.riskLevel(),
                        result.delayProbability(),
                        result.predictedDelayDays(),
                        OffsetDateTime.now(ZoneOffset.UTC)
                )
        );
    }
}