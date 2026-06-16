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

    /**
     * 기능: 예측 결과가 없는 미완료 주문을 찾아 지연 확률 예측 결과를 생성한다.
     *
     * Input:
     * - limit / int / 백필 대상으로 조회할 최대 주문 수
     *
     * Output:
     * - result / RiskPredictionCoverageResult / 대상 수, 성공 수, 실패 수, 성공 주문 ID, 실패 상세
     */
    public RiskPredictionCoverageResult backfillMissingPredictionsForIncompleteOrders(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive.");
        }

        List<Long> targetOrderIds =
                coverageRepository.findIncompleteOrderIdsMissingPrediction(limit);

        log.info(
                "Risk prediction missing backfill target loaded. targetCount={}, limit={}",
                targetOrderIds.size(),
                limit
        );

        return predictAndSaveForOrderIds(
                targetOrderIds,
                "missing-backfill"
        );
    }

    /**
     * 기능: 미완료 주문의 지연 확률 예측 결과를 최신 DB 기준으로 재생성한다.
     *
     * Input:
     * - limit / int / 재예측 대상으로 조회할 최대 주문 수
     *
     * Output:
     * - result / RiskPredictionCoverageResult / 대상 수, 성공 수, 실패 수, 성공 주문 ID, 실패 상세
     */
    public RiskPredictionCoverageResult refreshPredictionsForIncompleteOrders(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive.");
        }

        List<Long> targetOrderIds =
                coverageRepository.findIncompleteOrderIdsForPredictionRefresh(limit);

        log.info(
                "Risk prediction refresh target loaded. targetCount={}, limit={}",
                targetOrderIds.size(),
                limit
        );

        return predictAndSaveForOrderIds(
                targetOrderIds,
                "refresh"
        );
    }
    
    private RiskPredictionCoverageResult predictAndSaveForOrderIds(
            List<Long> targetOrderIds,
            String jobType
    ) {
        List<Long> succeededOrderIds = new ArrayList<>();
        List<RiskPredictionBackfillFailure> failures = new ArrayList<>();

        for (Long orderId : targetOrderIds) {
            try {
                AiPredictionResultSaveResult result =
                        riskPredictionService.predictAndSaveDelayProbability(orderId);

                succeededOrderIds.add(orderId);

                log.info(
                        "Risk prediction {} succeeded. orderId={}, predictionId={}, riskLevel={}, delayProbability={}, requiresAgentAnalysis={}",
                        jobType,
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

                log.error(
                        "Risk prediction {} failed. orderId={}",
                        jobType,
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
