package s_map.server.domain.dashboard.dto.res;

import s_map.server.domain.dashboard.repository.DashboardRepository.WeeklyScheduleRow;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record DashboardWeeklyScheduleResponse(
        LocalDate startDate,
        LocalDate endDate,
        List<WeeklyLineSchedule> lines
) {

    public static DashboardWeeklyScheduleResponse of(
            LocalDate startDate,
            LocalDate endDate,
            List<WeeklyScheduleRow> rows
    ) {
        Map<Long, List<WeeklyScheduleRow>> groupedByLine = rows.stream()
                .collect(Collectors.groupingBy(WeeklyScheduleRow::lineId));

        List<WeeklyLineSchedule> lines = groupedByLine.entrySet()
                .stream()
                .map(entry -> WeeklyLineSchedule.from(entry.getValue()))
                .sorted(Comparator.comparing(WeeklyLineSchedule::lineId))
                .toList();

        return new DashboardWeeklyScheduleResponse(startDate, endDate, lines);
    }

    public record WeeklyLineSchedule(
            Long lineId,
            String lineName,
            List<WeeklyScheduleItem> schedules
    ) {

        public static WeeklyLineSchedule from(List<WeeklyScheduleRow> rows) {
            WeeklyScheduleRow first = rows.get(0);

            return new WeeklyLineSchedule(
                    first.lineId(),
                    first.lineName(),
                    rows.stream()
                            .map(WeeklyScheduleItem::from)
                            .sorted(Comparator.comparing(WeeklyScheduleItem::plannedStartAt))
                            .toList()
            );
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
}