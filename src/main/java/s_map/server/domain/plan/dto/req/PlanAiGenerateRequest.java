package s_map.server.domain.plan.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Schema(description = "AI 생산계획 조정 결과 생성 요청")
public class PlanAiGenerateRequest {

    @Schema(description = "드래그 앤 드롭으로 이동하려는 생산계획 ID", example = "123")
    @NotNull(message = "생산계획 ID는 필수입니다.")
    @Positive(message = "생산계획 ID는 0보다 커야 합니다.")
    private Long planId;

    @Schema(description = "이동 목표 생산 라인 ID. 전달하지 않으면 기존 라인을 유지합니다.", example = "2", nullable = true)
    @Positive(message = "생산 라인 ID는 0보다 커야 합니다.")
    private Long lineId;

    @Schema(description = "이동 목표 계획 시작 일시", example = "2026-06-05T09:00:00+09:00")
    @NotNull(message = "계획 시작 일시는 필수입니다.")
    private OffsetDateTime plannedStartAt;

    @Schema(description = "이동 목표 계획 종료 일시", example = "2026-06-05T17:00:00+09:00")
    @NotNull(message = "계획 종료 일시는 필수입니다.")
    private OffsetDateTime plannedEndAt;

    @Schema(description = "AI 재계획 대상 기간 시작 일시", example = "2026-06-01T00:00:00+09:00")
    @NotNull(message = "재계획 시작 일시는 필수입니다.")
    private OffsetDateTime planningStart;

    @Schema(description = "AI 재계획 대상 기간 종료 일시", example = "2026-06-30T23:59:59+09:00")
    @NotNull(message = "재계획 종료 일시는 필수입니다.")
    private OffsetDateTime planningEnd;
}
