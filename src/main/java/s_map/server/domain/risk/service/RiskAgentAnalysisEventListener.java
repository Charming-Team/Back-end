package s_map.server.domain.risk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import s_map.server.domain.risk.dto.fastapi.FastApiRiskAgentExecuteResponse;

@Component
public class RiskAgentAnalysisEventListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RiskAgentAnalysisEventListener.class
            );

    private final RiskFastApiProperties properties;
    private final RiskAgentFastApiClient riskAgentFastApiClient;

    public RiskAgentAnalysisEventListener(
            RiskFastApiProperties properties,
            RiskAgentFastApiClient riskAgentFastApiClient
    ) {
        this.properties = properties;
        this.riskAgentFastApiClient = riskAgentFastApiClient;
    }

    @Async("riskAgentTaskExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handleRiskAgentAnalysisTrigger(
            RiskAgentAnalysisTriggerEvent event
    ) {
        if (!properties.riskAgentAnalysisEnabled()) {
            log.info(
                    "Risk Agent automatic analysis is disabled. "
                            + "predictionId={}, orderId={}, "
                            + "riskLevel={}, delayProbability={}, "
                            + "predictedDelayDays={}",
                    event.predictionId(),
                    event.orderId(),
                    event.riskLevel(),
                    event.delayProbability(),
                    event.predictedDelayDays()
            );
            return;
        }

        log.info(
                "Risk Agent automatic analysis started. "
                        + "predictionId={}, orderId={}, "
                        + "riskLevel={}, delayProbability={}, "
                        + "predictedDelayDays={}",
                event.predictionId(),
                event.orderId(),
                event.riskLevel(),
                event.delayProbability(),
                event.predictedDelayDays()
        );

        try {
            FastApiRiskAgentExecuteResponse response =
                    riskAgentFastApiClient.execute(
                            event.predictionId(),
                            event.orderId(),
                            event.triggeredAt()
                    );

            log.info(
                    "Risk Agent automatic analysis completed. "
                            + "predictionId={}, orderId={}, "
                            + "workflowRunId={}, status={}, "
                            + "causeTypes={}, retryCount={}",
                    event.predictionId(),
                    event.orderId(),
                    response.workflowRunId(),
                    response.status(),
                    response.selectedCauseTypes(),
                    response.retryCount()
            );

        } catch (Exception exception) {
            /*
             * Agent 실패가 이미 저장된 주문, 생산계획,
             * ML 예측 결과를 롤백시키지 않도록 예외를 전파하지 않습니다.
             */
            log.error(
                    "Risk Agent automatic analysis failed. "
                            + "predictionId={}, orderId={}, reason={}",
                    event.predictionId(),
                    event.orderId(),
                    exception.getMessage(),
                    exception
            );
        }
    }
}