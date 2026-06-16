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

    public RiskPredictionService(
            RiskFastApiClient riskFastApiClient,
            AiPredictionResultJdbcRepository aiPredictionResultJdbcRepository
    ) {
        this.riskFastApiClient = riskFastApiClient;
        this.aiPredictionResultJdbcRepository = aiPredictionResultJdbcRepository;
    }

    /**
     * 기능: 주문 ID 기준으로 기본 상위 요인 개수의 지연 확률 예측 결과를 생성하고 저장한다.
     *
     * Input:
     * - orderId / Long / 예측 대상 주문 고유 ID
     *
     * Output:
     * - result / AiPredictionResultSaveResult / 저장된 예측 결과 ID와 위험도, 지연 확률 요약
     */
    @Transactional
    public AiPredictionResultSaveResult predictAndSaveDelayProbability(Long orderId) {
        return predictAndSaveDelayProbability(orderId, DEFAULT_TOP_N);
    }

    /**
     * 기능: 주문 ID와 상위 요인 개수를 기준으로 지연 확률 예측 결과를 생성하고 저장한다.
     *
     * Input:
     * - orderId / Long / 예측 대상 주문 고유 ID
     * - topN / Integer / FastAPI 응답에 포함할 SHAP 상위 요인 개수
     *
     * Output:
     * - result / AiPredictionResultSaveResult / 저장된 예측 결과 ID와 위험도, 지연 확률 요약
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

        return AiPredictionResultSaveResult.from(
                predictionId,
                saveCommand
        );
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
