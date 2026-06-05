package s_map.server.domain.plan.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import s_map.server.domain.order.entity.PlanStatus;

import java.time.OffsetDateTime;

@Schema(description = "생산계획 수정 요청")
@Getter
public class PlanUpdateRequest {

    @Schema(description = "변경할 생산 라인 ID", example = "1")
    @NotNull(message = "생산 라인 ID는 필수입니다.")
    @Positive(message = "생산 라인 ID는 0보다 커야 합니다.")
    private Long lineId;

    @Schema(description = "변경할 생산 담당자 ID. 담당자를 미배정하려면 null로 전달합니다.", example = "12", nullable = true)
    @Positive(message = "생산 담당자 ID는 0보다 커야 합니다.")
    private Long operatorId;

    @Schema(description = "계획 시작 일시", example = "2026-06-05T09:00:00+09:00")
    @NotNull(message = "계획 시작 일시는 필수입니다.")
    private OffsetDateTime plannedStartAt;

    @Schema(description = "계획 종료 일시", example = "2026-06-05T17:00:00+09:00")
    @NotNull(message = "계획 종료 일시는 필수입니다.")
    private OffsetDateTime plannedEndAt;

    @Schema(description = "계획 생산 수량", example = "5000")
    @NotNull(message = "계획 수량은 필수입니다.")
    @Positive(message = "계획 수량은 0보다 커야 합니다.")
    private Integer plannedQuantity;

    @Schema(description = "라인 내 생산 순서", example = "3")
    @NotNull(message = "라인 내 생산 순서는 필수입니다.")
    @Positive(message = "라인 내 생산 순서는 0보다 커야 합니다.")
    private Integer planSequence;

    @Schema(
            description = "생산계획 상태",
            example = "SCHEDULED",
            allowableValues = {"SCHEDULED", "IN_PROGRESS", "COMPLETED", "DELAYED", "CANCELLED"}
    )
    @NotNull(message = "생산계획 상태는 필수입니다.")
    private PlanStatus planStatus;
}
