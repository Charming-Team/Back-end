package s_map.server.domain.dashboard.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import s_map.server.domain.dashboard.repository.DashboardRepository.WeeklyScheduleRow;
import s_map.server.domain.dashboard.repository.DashboardRepository.WeeklyScheduleSegmentRow;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Schema(description = "주간 생산 스케줄 응답")
public record DashboardWeeklyScheduleResponse(
        @Schema(description = "조회 시작일", example = "2026-06-01")
        LocalDate startDate,

        @Schema(description = "조회 종료일", example = "2026-06-07")
        LocalDate endDate,

        @Schema(description = "라인별 주간 생산 스케줄")
        List<WeeklyLineSchedule> lines
) {

    public static DashboardWeeklyScheduleResponse of(
            LocalDate startDate,
            LocalDate endDate,
            List<WeeklyScheduleRow> rows,
            List<WeeklyScheduleSegmentRow> segmentRows
    ) {
        Map<Long, List<WeeklyScheduleRow>> groupedByLine = rows.stream()
                .collect(Collectors.groupingBy(WeeklyScheduleRow::lineId));
        Map<Long, List<WeeklyScheduleSegmentRow>> segmentsGroupedByLine = segmentRows.stream()
                .collect(Collectors.groupingBy(WeeklyScheduleSegmentRow::lineId));

        Set<Long> lineIds = Stream.concat(groupedByLine.keySet().stream(), segmentsGroupedByLine.keySet().stream())
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<WeeklyLineSchedule> lines = lineIds.stream()
                .map(lineId -> WeeklyLineSchedule.from(
                        lineId,
                        groupedByLine.getOrDefault(lineId, List.of()),
                        segmentsGroupedByLine.getOrDefault(lineId, List.of())
                ))
                .sorted(Comparator.comparing(WeeklyLineSchedule::lineId))
                .toList();

        return new DashboardWeeklyScheduleResponse(startDate, endDate, lines);
    }

    public record WeeklyLineSchedule(
            @Schema(description = "라인 ID", example = "1")
            Long lineId,

            @Schema(description = "라인명", example = "Line A")
            String lineName,

            @Schema(description = "생산계획 단위 스케줄 목록. 기존 계획 막대 표시에 사용합니다.")
            List<WeeklyScheduleItem> schedules,

            @Schema(description = "차트 표시용 상태 구간 목록. PLAN은 계획 기준 구간, LINE_STATUS는 라인 상태 기록 기준 구간입니다.")
            List<WeeklyScheduleSegment> segments
    ) {

        public static WeeklyLineSchedule from(
                Long lineId,
                List<WeeklyScheduleRow> rows,
                List<WeeklyScheduleSegmentRow> segmentRows
        ) {
            String lineName = resolveLineName(rows, segmentRows);

            return new WeeklyLineSchedule(
                    lineId,
                    lineName,
                    rows.stream()
                            .map(WeeklyScheduleItem::from)
                            .sorted(Comparator.comparing(WeeklyScheduleItem::plannedStartAt))
                            .toList(),
                    segmentRows.stream()
                            .map(WeeklyScheduleSegment::from)
                            .sorted(Comparator.comparing(WeeklyScheduleSegment::segmentStartAt))
                            .toList()
            );
        }

        private static String resolveLineName(
                List<WeeklyScheduleRow> rows,
                List<WeeklyScheduleSegmentRow> segmentRows
        ) {
            if (!rows.isEmpty()) {
                return rows.get(0).lineName();
            }

            if (!segmentRows.isEmpty()) {
                return segmentRows.get(0).lineName();
            }

            return null;
        }
    }

    public record WeeklyScheduleItem(
            @Schema(description = "생산계획 ID", example = "1001")
            Long planId,

            @Schema(description = "주문 ID", example = "2001")
            Long orderId,

            @Schema(description = "주문 번호", example = "PO-240520-001")
            String orderNo,

            @Schema(description = "제품명", example = "ABS-Black")
            String productName,

            @Schema(description = "계획 시작 시각", example = "2026-06-01T09:00:00+09:00")
            OffsetDateTime plannedStartAt,

            @Schema(description = "계획 종료 시각", example = "2026-06-01T18:00:00+09:00")
            OffsetDateTime plannedEndAt,

            @Schema(description = "생산계획 상태", example = "IN_PROGRESS")
            String planStatus,

            @Schema(description = "해당 계획의 최신 라인 가동 상태. 상태 기록이 없으면 null", example = "RUNNING", nullable = true)
            String operationStatus,

            @Schema(description = "화면 표시 상태", example = "생산 중")
            String displayStatus
    ) {

        public static WeeklyScheduleItem from(WeeklyScheduleRow row) {
            return new WeeklyScheduleItem(
                    row.planId(),
                    row.orderId(),
                    row.orderNo(),
                    row.productName(),
                    row.plannedStartAt(),
                    row.plannedEndAt(),
                    row.planStatus(),
                    row.operationStatus(),
                    toDisplayStatus(row.planStatus(), row.operationStatus())
            );
        }

        private static String toDisplayStatus(String planStatus, String operationStatus) {
            if ("COMPLETED".equals(planStatus)) {
                return "완료";
            }

            if ("DELAYED".equals(planStatus)) {
                return "지연";
            }

            if ("SETUP".equals(operationStatus)) {
                return "셋업";
            }

            if ("STOPPED".equals(operationStatus)
                    || "ERROR".equals(operationStatus)
                    || "MAINTENANCE".equals(operationStatus)
                    || "IDLE".equals(operationStatus)) {
                return "비가동";
            }

            if ("IN_PROGRESS".equals(planStatus) || "RUNNING".equals(operationStatus)) {
                return "생산 중";
            }

            if ("SCHEDULED".equals(planStatus)) {
                return "예정";
            }

            return "확인 필요";
        }
    }

    public record WeeklyScheduleSegment(
            @Schema(description = "생산계획 ID. 라인 상태 구간이 특정 계획과 연결되지 않으면 null", example = "1001", nullable = true)
            Long planId,

            @Schema(description = "주문 ID. 연결된 생산계획이 없으면 null", example = "2001", nullable = true)
            Long orderId,

            @Schema(description = "주문 번호. 연결된 생산계획이 없으면 null", example = "PO-240520-001", nullable = true)
            String orderNo,

            @Schema(description = "제품명. 연결된 생산계획 또는 라인 상태 제품 정보가 없으면 null", example = "ABS-Black", nullable = true)
            String productName,

            @Schema(description = "구간 시작 시각", example = "2026-06-01T09:00:00+09:00")
            OffsetDateTime segmentStartAt,

            @Schema(description = "구간 종료 시각", example = "2026-06-01T11:00:00+09:00")
            OffsetDateTime segmentEndAt,

            @Schema(description = "생산계획 상태. PLAN 구간 또는 계획과 연결된 LINE_STATUS 구간에서 제공", example = "IN_PROGRESS", nullable = true)
            String planStatus,

            @Schema(description = "라인 가동 상태. LINE_STATUS 구간에서 제공", example = "RUNNING", nullable = true)
            String operationStatus,

            @Schema(description = "구간 유형", example = "LINE_STATUS", allowableValues = {"PLAN", "LINE_STATUS"})
            String segmentType,

            @Schema(description = "화면 표시 상태", example = "생산 중", allowableValues = {"생산 중", "예정", "셋업", "지연", "비가동", "완료", "확인 필요"})
            String displayStatus
    ) {

        public static WeeklyScheduleSegment from(WeeklyScheduleSegmentRow row) {
            return new WeeklyScheduleSegment(
                    row.planId(),
                    row.orderId(),
                    row.orderNo(),
                    row.productName(),
                    row.segmentStartAt(),
                    row.segmentEndAt(),
                    row.planStatus(),
                    row.operationStatus(),
                    row.segmentType(),
                    toDisplayStatus(row.planStatus(), row.operationStatus())
            );
        }

        private static String toDisplayStatus(String planStatus, String operationStatus) {
            if ("SETUP".equals(operationStatus)) {
                return "셋업";
            }

            if ("STOPPED".equals(operationStatus)
                    || "ERROR".equals(operationStatus)
                    || "MAINTENANCE".equals(operationStatus)
                    || "IDLE".equals(operationStatus)) {
                return "비가동";
            }

            if ("RUNNING".equals(operationStatus)) {
                return "생산 중";
            }

            if ("DELAYED".equals(planStatus)) {
                return "지연";
            }

            if ("IN_PROGRESS".equals(planStatus)) {
                return "생산 중";
            }

            if ("SCHEDULED".equals(planStatus)) {
                return "예정";
            }

            if ("COMPLETED".equals(planStatus)) {
                return "완료";
            }

            return "확인 필요";
        }
    }
}
