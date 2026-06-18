package s_map.server.domain.plan.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "사용자 선택 생산계획 시뮬레이션 저장 응답")
@Getter
@Builder
public class PlanSimulationSelectedSaveResponse {

    @Schema(description = "저장된 시뮬레이션 결과 ID", example = "1")
    private Long simulationId;

    @Schema(description = "시뮬레이션 그룹 ID", example = "SIM-GRP-20260609-001")
    private String simulationGroupId;

    @Schema(description = "저장된 생산계획 수", example = "3")
    private Integer savedPlanCount;

    @Schema(description = "저장된 시뮬레이션 상세 수", example = "3")
    private Integer savedDetailCount;

    @Schema(description = "저장된 생산계획 ID 목록")
    private List<Long> savedPlanIds;

    @Schema(description = "적용 여부", example = "true")
    private boolean applied;

    @Schema(description = "적용 사용자 ID", example = "7")
    private Long appliedBy;

    @Schema(description = "적용 일시", example = "2026-06-09T10:30:00+09:00")
    private OffsetDateTime appliedAt;
}