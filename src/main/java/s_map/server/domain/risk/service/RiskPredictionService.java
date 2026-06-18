package s_map.server.domain.risk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.risk.dto.fastapi.FastApiDelayPredictionResponse;
import s_map.server.domain.risk.dto.fastapi.FastApiDelayProbabilityResponse;
import s_map.server.domain.risk.dto.internal.AiPredictionResultSaveCommand;
import s_map.server.domain.risk.dto.internal.AiPredictionResultSaveResult;
import s_map.server.domain.risk.repository.AiPredictionResultJdbcRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

@Service
public class RiskPredictionService {

    private static final Logger log = LoggerFactory.getLogger(RiskPredictionService.class);

    private static final int DEFAULT_TOP_N = 5;
    private static final int MIN_TOP_N = 1;
    private static final int MAX_TOP_N = 20;

    private final RiskFastApiClient riskFastApiClient;
    private final AiPredictionResultJdbcRepository aiPredictionResultJdbcRepository;
    private final RiskAgentAnalysisEventPublisher riskAgentAnalysisEventPublisher;

    public RiskPredictionService(
            RiskFastApiClient riskFastApiClient,
            AiPredictionResultJdbcRepository aiPredictionResultJdbcRepository,
            RiskAgentAnalysisEventPublisher riskAgentAnalysisEventPublisher
    ) {
        this.riskFastApiClient = riskFastApiClient;
        this.aiPredictionResultJdbcRepository = aiPredictionResultJdbcRepository;
        this.riskAgentAnalysisEventPublisher = riskAgentAnalysisEventPublisher;
    }

    /**
     * 기능: 주문 ID 기준 지연 확률을 기본 원인 개수로 예측하고 결과를 저장한다.
     *
     * Input:
     * - orderId / Long / 지연 위험을 예측할 주문 ID
     *
     * Output:
     * - result / AiPredictionResultSaveResult / 저장된 AI 예측 결과 요약
     */
    @Transactional
    public AiPredictionResultSaveResult predictAndSaveDelayProbability(Long orderId) {
        return predictAndSaveDelayProbability(orderId, DEFAULT_TOP_N);
    }

    /**
     * 기능: 주문 ID 기준 지연 확률과 지연 예상일을 예측하고 결과를 저장한다.
     *
     * Input:
     * - orderId / Long / 지연 위험을 예측할 주문 ID
     * - topN / Integer / FastAPI가 반환할 주요 원인 최대 개수
     *
     * Output:
     * - result / AiPredictionResultSaveResult / 저장된 AI 예측 결과 요약
     */
    @Transactional
    public AiPredictionResultSaveResult predictAndSaveDelayProbability(
            Long orderId,
            Integer topN
    ) {
        validateOrderId(orderId);

        int normalizedTopN = normalizeTopN(topN);

        FastApiDelayProbabilityResponse probabilityResponse =
                riskFastApiClient.predictDelayProbability(orderId, normalizedTopN);

        BigDecimal predictedDelayDays = predictDelayDaysOrNull(orderId);

        AiPredictionResultSaveCommand saveCommand =
                probabilityResponse.toSaveCommand(predictedDelayDays);

        Long predictionId = aiPredictionResultJdbcRepository.save(saveCommand);

        AiPredictionResultSaveResult saveResult = AiPredictionResultSaveResult.from(
                predictionId,
                saveCommand
        );

        riskAgentAnalysisEventPublisher.publishIfRequired(saveResult);

        return saveResult;
    }

    @Transactional
    public boolean predictAndUpdateDelayDays(
            Long predictionId,
            Long orderId
    ) {
        Objects.requireNonNull(predictionId, "predictionId must not be null");
        validateOrderId(orderId);

        BigDecimal predictedDelayDays = predictDelayDaysOrNull(orderId);

        if (predictedDelayDays == null) {
            return false;
        }

        int updatedCount = aiPredictionResultJdbcRepository.updatePredictedDelayDays(
                predictionId,
                predictedDelayDays
        );

        return updatedCount == 1;
    }

    private BigDecimal predictDelayDaysOrNull(Long orderId) {
        try {
            FastApiDelayPredictionResponse delayPredictionResponse =
                    riskFastApiClient.predictDelayHours(orderId);

            BigDecimal predictedDelayHours = delayPredictionResponse.predictedDelayHours();

            if (predictedDelayHours == null) {
                return null;
            }

            BigDecimal predictedDelayDays = predictedDelayHours
                    .max(BigDecimal.ZERO)
                    .divide(BigDecimal.valueOf(24), 2, RoundingMode.HALF_UP);

            log.info(
                    "Delay days prediction succeeded. orderId={}, predictedDelayHours={}, predictedDelayDays={}, delayPredictionModelName={}",
                    orderId,
                    predictedDelayHours,
                    predictedDelayDays,
                    delayPredictionResponse.modelName()
            );

            return predictedDelayDays;

        } catch (Exception ex) {
            log.warn(
                    "Delay days prediction failed. orderId={}, reason={}",
                    orderId,
                    ex.getMessage()
            );

            return null;
        }
    }

    private static void validateOrderId(Long orderId) {
        Objects.requireNonNull(orderId, "orderId must not be null");

        if (orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive.");
        }
    }

    private static int normalizeTopN(Integer topN) {
        if (topN == null) {
            return DEFAULT_TOP_N;
        }

        if (topN < MIN_TOP_N || topN > MAX_TOP_N) {
            throw new IllegalArgumentException("topN must be between 1 and 20.");
        }

        return topN;
    }
}
