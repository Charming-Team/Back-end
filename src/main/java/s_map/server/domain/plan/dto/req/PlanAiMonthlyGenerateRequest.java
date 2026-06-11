package s_map.server.domain.plan.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Schema(description = "월간 AI 생산계획 분석 요청")
public class PlanAiMonthlyGenerateRequest {

    @Schema(description = "월간 재계획 대상 기간 시작 일시", example = "2026-06-01T00:00:00+09:00")
    @NotNull(message = "재계획 시작 일시는 필수입니다.")
    private OffsetDateTime planningStart;

    @Schema(description = "월간 재계획 대상 기간 종료 일시", example = "2026-07-01T00:00:00+09:00")
    @NotNull(message = "재계획 종료 일시는 필수입니다.")
    private OffsetDateTime planningEnd;
}
