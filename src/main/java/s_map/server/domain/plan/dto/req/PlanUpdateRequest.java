package s_map.server.domain.plan.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import s_map.server.domain.order.entity.PlanStatus;

import java.time.OffsetDateTime;

@Getter
public class PlanUpdateRequest {

    @NotNull(message = "생산 라인 ID는 필수입니다.")
    @Positive(message = "생산 라인 ID는 0보다 커야 합니다.")
    private Long lineId;

    @Positive(message = "생산 담당자 ID는 0보다 커야 합니다.")
    private Long operatorId;

    @NotNull(message = "계획 시작 일시는 필수입니다.")
    private OffsetDateTime plannedStartAt;

    @NotNull(message = "계획 종료 일시는 필수입니다.")
    private OffsetDateTime plannedEndAt;

    @NotNull(message = "계획 수량은 필수입니다.")
    @Positive(message = "계획 수량은 0보다 커야 합니다.")
    private Integer plannedQuantity;

    @NotNull(message = "라인 내 생산 순서는 필수입니다.")
    @Positive(message = "라인 내 생산 순서는 0보다 커야 합니다.")
    private Integer planSequence;

    @NotNull(message = "생산계획 상태는 필수입니다.")
    private PlanStatus planStatus;
}
