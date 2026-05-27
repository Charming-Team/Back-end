package s_map.server.domain.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlanStatus {
    SCHEDULED("예정"),
    IN_PROGRESS("진행 중"),
    COMPLETED("완료"),
    DELAYED("지연"),
    CANCELLED("취소");

    private final String label;
}