package s_map.server.domain.plan.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "생산계획 시뮬레이션 상세 변경 내역 응답")
@Getter
@Builder
public class PlanSimulationDetailResponse {

    @Schema(description = "시뮬레이션 상세 변경 내역 ID", example = "1")
    private Long simulationDetailId;
    @Schema(description = "시뮬레이션 결과 ID", example = "1")
    private Long simulationId;

    @Schema(description = "변경 대상 생산계획 ID", example = "25")
    private Long planId;
    @Schema(description = "변경 대상 주문 ID", example = "10")
    private Long orderId;

    @Schema(description = "변경 전 생산 라인 ID", example = "2", nullable = true)
    private Long beforeLineId;
    @Schema(description = "변경 전 생산 라인명", example = "ABS 보조 생산 Line", nullable = true)
    private String beforeLineName;

    @Schema(description = "변경 후 생산 라인 ID", example = "4", nullable = true)
    private Long afterLineId;
    @Schema(description = "변경 후 생산 라인명", example = "PP 기능성 생산 Line", nullable = true)
    private String afterLineName;

    @Schema(description = "변경 전 라인 내 생산 순서", example = "3", nullable = true)
    private Integer beforeSequence;
    @Schema(description = "변경 후 라인 내 생산 순서", example = "1", nullable = true)
    private Integer afterSequence;

    @Schema(description = "변경 전 계획 시작 일시", example = "2026-06-05T09:00:00+09:00", nullable = true)
    private OffsetDateTime beforeStartAt;
    @Schema(description = "변경 전 계획 종료 일시", example = "2026-06-05T17:00:00+09:00", nullable = true)
    private OffsetDateTime beforeEndAt;

    @Schema(description = "변경 후 계획 시작 일시", example = "2026-06-05T08:00:00+09:00")
    private OffsetDateTime afterStartAt;
    @Schema(description = "변경 후 계획 종료 일시", example = "2026-06-05T16:00:00+09:00")
    private OffsetDateTime afterEndAt;

    @Schema(description = "변경 후 예상 완료일", example = "2026-06-05", nullable = true)
    private LocalDate expectedCompletionDate;
    @Schema(description = "변경 후 납기 지연 여부", example = "false")
    private Boolean afterDelayed;

    @Schema(description = "변경 전 계획 생산 수량", example = "5000", nullable = true)
    private Integer beforeQuantity;
    @Schema(description = "변경 후 계획 생산 수량", example = "5000")
    private Integer afterQuantity;

    @Schema(description = "변경 사유", example = "병목 라인 부하 분산", nullable = true)
    private String changeReason;

    @Schema(description = "상세 변경 내역 생성 일시", example = "2026-06-05T13:30:00+09:00")
    private OffsetDateTime createdAt;
}
