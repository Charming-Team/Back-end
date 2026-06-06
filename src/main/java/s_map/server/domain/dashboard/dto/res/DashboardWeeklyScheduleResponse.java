package s_map.server.domain.dashboard.dto.res;

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

public record DashboardWeeklyScheduleResponse(
        LocalDate startDate,
        LocalDate endDate,
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
            Long lineId,
            String lineName,
            List<WeeklyScheduleItem> schedules,
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
            Long planId,
            Long orderId,
            String orderNo,
            String productName,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            String planStatus,
            String operationStatus,
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
            Long planId,
            Long orderId,
            String orderNo,
            String productName,
            OffsetDateTime segmentStartAt,
            OffsetDateTime segmentEndAt,
            String planStatus,
            String operationStatus,
            String segmentType,
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
