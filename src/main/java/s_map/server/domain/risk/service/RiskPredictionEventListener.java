package s_map.server.domain.risk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import s_map.server.domain.risk.repository.RiskPredictionCoverageRepository;

@Component
public class RiskPredictionEventListener {

    private static final Logger log = LoggerFactory.getLogger(RiskPredictionEventListener.class);

    private final RiskPredictionService riskPredictionService;
    private final RiskPredictionCoverageRepository coverageRepository;

    public RiskPredictionEventListener(
            RiskPredictionService riskPredictionService,
            RiskPredictionCoverageRepository coverageRepository
    ) {
        this.riskPredictionService = riskPredictionService;
        this.coverageRepository = coverageRepository;
    }

    /**
     * 주문/생산계획 변경 트랜잭션이 commit된 뒤 지연 확률 예측을 실행합니다.
     *
     * 이유:
     * - FastAPI는 DB inference view를 조회합니다.
     * - 주문/계획 변경 내용이 commit되기 전에 FastAPI가 호출되면 이전 데이터를 읽을 수 있습니다.
     *
     * fallbackExecution = true:
     * - 트랜잭션 밖에서 이벤트가 발행된 경우에도 즉시 실행합니다.
     * - 다만 실제 주문/계획 트리거 연결 시에는 @Transactional 메서드 내부에서 publish하는 것을 권장합니다.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true
    )
    @Async("riskPredictionTaskExecutor")
    public void handleRiskPredictionTrigger(RiskPredictionTriggerEvent event) {
        log.info(
                "Risk prediction trigger received. orderId={}, triggerType={}, triggeredAt={}",
                event.orderId(),
                event.triggerType(),
                event.triggeredAt()
        );

        try {
            if (!coverageRepository.existsIncompleteOrder(event.orderId())) {
                log.info(
                        "Risk prediction skipped because order is completed/cancelled or not found. orderId={}, triggerType={}",
                        event.orderId(),
                        event.triggerType()
                );
                return;
            }

            var result = riskPredictionService.predictAndSaveDelayProbability(
                    event.orderId()
            );

            log.info(
                    "Risk prediction completed. predictionId={}, orderId={}, riskLevel={}, delayProbability={}, requiresAgentAnalysis={}",
                    result.predictionId(),
                    result.orderId(),
                    result.riskLevel(),
                    result.delayProbability(),
                    result.requiresAgentAnalysis()
            );

        } catch (Exception ex) {
            /*
             * 현재 단계에서는 원 주문/계획 트랜잭션을 실패시키지 않습니다.
             * 추후 재시도 job 또는 실패 이력 테이블을 추가할 수 있습니다.
             */
            log.error(
                    "Risk prediction failed. orderId={}, triggerType={}",
                    event.orderId(),
                    event.triggerType(),
                    ex
            );
        }
    }
}
