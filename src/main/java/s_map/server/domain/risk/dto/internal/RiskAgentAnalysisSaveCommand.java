package s_map.server.domain.risk.dto.internal;

import s_map.server.domain.risk.entity.DelayCauseType;

import java.util.List;
import java.util.Objects;

public record RiskAgentAnalysisSaveCommand(
        Long predictionId,
        Long orderId,
        String analysisSummary,
        String recommendedAction,
        List<DelayCauseType> causeTypes
) {

    public RiskAgentAnalysisSaveCommand {
        Objects.requireNonNull(predictionId, "predictionId must not be null");
        Objects.requireNonNull(orderId, "orderId must not be null");
        
        if (orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive.");
        }

        if (predictionId <= 0) {
            throw new IllegalArgumentException("predictionId must be positive.");
        }

        if (analysisSummary == null || analysisSummary.isBlank()) {
            throw new IllegalArgumentException("analysisSummary must not be blank.");
        }

        analysisSummary = analysisSummary.trim();

        if (recommendedAction != null && recommendedAction.isBlank()) {
            recommendedAction = null;
        }

        if (recommendedAction != null) {
            recommendedAction = recommendedAction.trim();
        }

        causeTypes = causeTypes == null
                ? List.of()
                : List.copyOf(causeTypes);
    }
}