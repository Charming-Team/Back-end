package s_map.server.domain.line.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OperationStatus {
    RUNNING("가동 중"),
    IDLE("대기"),
    SETUP("전환/준비 중"),
    STOPPED("정지"),
    ERROR("오류"),
    MAINTENANCE("점검");

    private final String label;
}
