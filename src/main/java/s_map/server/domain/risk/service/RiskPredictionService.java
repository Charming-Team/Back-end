package s_map.server.domain.risk.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.risk.dto.fastapi.FastApiDelayProbabilityResponse;
import s_map.server.domain.risk.dto.internal.AiPredictionResultSaveCommand;
import s_map.server.domain.risk.dto.internal.AiPredictionResultSaveResult;
import s_map.server.domain.risk.repository.AiPredictionResultJdbcRepository;

import java.util.Objects;

@Service
public class RiskPredictionService {

    private static final int DEFAULT_TOP_N = 5;
    private static final int MIN_TOP_N = 1;
    private static final int MAX_TOP_N = 20;

    private final RiskFastApiClient riskFastApiClient;
    private final AiPredictionResultJdbcRepository aiPredictionResultJdbcRepository;
    private final ObjectMapper objectMapper;

    public RiskPredictionService(
            RiskFastApiClient riskFastApiClient,
            AiPredictionResultJdbcRepository aiPredictionResultJdbcRepository,
            ObjectMapper objectMapper
    ) {
        this.riskFastApiClient = riskFastApiClient;
        this.aiPredictionResultJdbcRepository = aiPredictionResultJdbcRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiPredictionResultSaveResult predictAndSaveDelayProbability(Long orderId) {
        return predictAndSaveDelayProbability(orderId, DEFAULT_TOP_N);
    }

    @Transactional
    public AiPredictionResultSaveResult predictAndSaveDelayProbability(
            Long orderId,
            Integer topN
    ) {
        validateOrderId(orderId);

        int normalizedTopN = normalizeTopN(topN);

        FastApiDelayProbabilityResponse fastApiResponse =
                riskFastApiClient.predictDelayProbability(orderId, normalizedTopN);

        AiPredictionResultSaveCommand saveCommand =
                fastApiResponse.toSaveCommand(objectMapper);

        Long predictionId = aiPredictionResultJdbcRepository.save(saveCommand);

        /*
         * 현재 단계에서는 Agent를 호출하지 않습니다.
         *
         * saveResult.requiresAgentAnalysis() == true 인 경우:
         * - risk_level != SAFE
         * - 이후 AI Risk Analysis Agent 분석 대상입니다.
         */
        return AiPredictionResultSaveResult.from(
                predictionId,
                saveCommand
        );
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