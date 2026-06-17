package s_map.server.domain.plan.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateResponse;

@Schema(description = "AI 생산계획 조정 응답")
public record PlanAiGenerateResponse(
        @Schema(description = "AI 생산계획 후보 응답")
        @JsonProperty("planning_response")
        Object planningResponse,

        @Schema(description = "AI 시뮬레이션 응답")
        @JsonProperty("simulation_response")
        Object simulationResponse
) {

    public static PlanAiGenerateResponse from(FastApiPlanningGenerateResponse response) {
        return new PlanAiGenerateResponse(
                response.getPlanningResponse(),
                response.getSimulationResponse()
        );
    }
}
