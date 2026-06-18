package s_map.server.domain.risk.dto.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import s_map.server.domain.risk.entity.DelayCauseType;

import java.util.List;

public record RiskAgentAnalysisPersistRequest(
        @NotBlank
        String workflowRunId,

        @NotNull
        @Positive
        Long predictionId,

        @NotNull
        @Positive
        Long orderId,

        @NotBlank
        String analysisSummary,

        @NotBlank
        String recommendedAction,

        @NotEmpty
        List<@NotNull DelayCauseType> causeTypes
) {

    public RiskAgentAnalysisSaveCommand toCommand() {
        return new RiskAgentAnalysisSaveCommand(
                predictionId,
                orderId,
                analysisSummary,
                recommendedAction,
                causeTypes
        );
    }
}