package s_map.server.domain.plan.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

import java.time.OffsetDateTime;

@Schema(description = "생산계획 드래그 앤 드롭 일정 이동 요청")
@Getter
public class PlanScheduleUpdateRequest {

    @Schema(
            description = "이동할 생산 라인 ID. 전달하지 않으면 기존 라인을 유지합니다.",
            example = "2",
            nullable = true
    )
    @Positive(message = "생산 라인 ID는 0보다 커야 합니다.")
    private Long lineId;

    @Schema(description = "이동 후 계획 시작 일시", example = "2026-06-05T09:00:00+09:00")
    @NotNull(message = "계획 시작 일시는 필수입니다.")
    private OffsetDateTime plannedStartAt;

    @Schema(description = "이동 후 계획 종료 일시", example = "2026-06-05T17:00:00+09:00")
    @NotNull(message = "계획 종료 일시는 필수입니다.")
    private OffsetDateTime plannedEndAt;
}
