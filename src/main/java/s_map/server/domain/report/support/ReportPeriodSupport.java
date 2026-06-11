package s_map.server.domain.report.support;

import s_map.server.domain.report.entity.ReportType;

import java.time.LocalDate;

public final class ReportPeriodSupport {

    private ReportPeriodSupport() {
    }

    public static ResolvedPeriod resolve(ReportType reportType, LocalDate startDate, LocalDate endDate) {
        if (!isMonthlyReport(reportType)) {
            return new ResolvedPeriod(startDate, endDate);
        }

        LocalDate baseDate = startDate != null ? startDate : endDate;
        if (baseDate == null) {
            return new ResolvedPeriod(startDate, endDate);
        }

        LocalDate monthStart = baseDate.withDayOfMonth(1);
        return new ResolvedPeriod(monthStart, monthStart.withDayOfMonth(monthStart.lengthOfMonth()));
    }

    public static boolean isMonthlyReport(ReportType reportType) {
        return reportType == ReportType.MONTHLY || reportType == ReportType.MONTHLY_BUSINESS;
    }

    public record ResolvedPeriod(
            LocalDate startDate,
            LocalDate endDate
    ) {
        public LocalDate endDateExclusive() {
            return endDate != null ? endDate.plusDays(1) : null;
        }
    }
}
