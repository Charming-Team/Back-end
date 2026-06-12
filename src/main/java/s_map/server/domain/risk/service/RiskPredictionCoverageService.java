package s_map.server.domain.risk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import s_map.server.domain.risk.dto.internal.AiPredictionResultSaveResult;
import s_map.server.domain.risk.repository.RiskPredictionCoverageRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RiskPredictionCoverageService {

    private static final Logger log = LoggerFactory.getLogger(RiskPredictionCoverageService.class);

    private final RiskPredictionCoverageRepository coverageRepository;
    private final RiskPredictionService riskPredictionService;

    public RiskPredictionCoverageService(
            RiskPredictionCoverageRepository coverageRepository,
            RiskPredictionService riskPredictionService
    ) {
        this.coverageRepository = coverageRepository;
        this.riskPredictionService = riskPredictionService;
    }

    public RiskPredictionCoverageResult backfillMissingPredictionsForIncompleteOrders(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive.");
        }

        List<Long> targetOrderIds = coverageRepository.findIncompleteOrderIdsMissingPrediction(limit);

        log.info(
                "Risk prediction backfill target loaded. targetCount={}, limit={}",
                targetOrderIds.size(),
                limit
        );

        List<Long> succeededOrderIds = new ArrayList<>();
        List<RiskPredictionBackfillFailure> failures = new ArrayList<>();

        for (Long orderId : targetOrderIds) {
            try {
                AiPredictionResultSaveResult result =
                        riskPredictionService.predictAndSaveDelayProbability(orderId);

                succeededOrderIds.add(orderId);

                log.info(
                        "Risk prediction backfill succeeded. orderId={}, predictionId={}, riskLevel={}, delayProbability={}, requiresAgentAnalysis={}",
                        orderId,
                        result.predictionId(),
                        result.riskLevel(),
                        result.delayProbability(),
                        result.requiresAgentAnalysis()
                );

            } catch (Exception ex) {
                failures.add(
                        new RiskPredictionBackfillFailure(
                                orderId,
                                ex.getClass().getSimpleName(),
                                ex.getMessage()
                        )
                );

                /*
                 * 일부 주문 실패가 전체 backfill을 중단하지 않도록 합니다.
                 */
                log.error(
                        "Risk prediction backfill failed. orderId={}",
                        orderId,
                        ex
                );
            }
        }

        return new RiskPredictionCoverageResult(
                targetOrderIds.size(),
                succeededOrderIds.size(),
                failures.size(),
                succeededOrderIds,
                failures
        );
    }

    public record RiskPredictionCoverageResult(
            int targetCount,
            int succeededCount,
            int failedCount,
            List<Long> succeededOrderIds,
            List<RiskPredictionBackfillFailure> failures
    ) {
        public RiskPredictionCoverageResult {
            Objects.requireNonNull(succeededOrderIds, "succeededOrderIds must not be null");
            Objects.requireNonNull(failures, "failures must not be null");
        }
    }

    public record RiskPredictionBackfillFailure(
            Long orderId,
            String errorType,
            String message
    ) {
        public RiskPredictionBackfillFailure {
            Objects.requireNonNull(orderId, "orderId must not be null");
            Objects.requireNonNull(errorType, "errorType must not be null");
        }
    }
}