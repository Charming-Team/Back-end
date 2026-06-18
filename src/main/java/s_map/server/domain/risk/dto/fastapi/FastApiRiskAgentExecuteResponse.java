package s_map.server.domain.risk.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FastApiRiskAgentExecuteResponse(

        @JsonProperty("workflowRunId")
        String workflowRunId,

        @JsonProperty("predictionId")
        Long predictionId,

        @JsonProperty("orderId")
        Long orderId,

        @JsonProperty("status")
        String status,

        @JsonProperty("selectedCauseTypes")
        List<String> selectedCauseTypes,

        @JsonProperty("retryCount")
        Integer retryCount,

        @JsonProperty("finishedAt")
        OffsetDateTime finishedAt,

        @JsonProperty("errorMessage")
        String errorMessage
) {

    public boolean isPersisted() {
        return "PERSISTED".equals(status);
    }
}