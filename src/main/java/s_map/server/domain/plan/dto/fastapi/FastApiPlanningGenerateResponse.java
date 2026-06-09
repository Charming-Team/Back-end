package s_map.server.domain.plan.dto.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "FastAPI 생산계획 조정 응답")
public class FastApiPlanningGenerateResponse {

    @Schema(description = "FastAPI 생산계획 후보 응답")
    @JsonProperty("planning_response")
    private Object planningResponse;

    @Schema(description = "FastAPI 시뮬레이션 응답")
    @JsonProperty("simulation_response")
    private Object simulationResponse;
}
