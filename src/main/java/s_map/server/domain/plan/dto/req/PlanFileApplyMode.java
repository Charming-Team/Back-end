package s_map.server.domain.plan.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "생산계획 파일 반영 방식")
public enum PlanFileApplyMode {
    INITIAL_REGISTER,
    FULL_REPLACE
}
