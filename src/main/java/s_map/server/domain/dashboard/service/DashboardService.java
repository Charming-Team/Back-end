package s_map.server.domain.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.dashboard.dto.res.DashboardLineUtilizationResponse;
import s_map.server.domain.dashboard.dto.res.DashboardOrderDeliveryStatusResponse;
import s_map.server.domain.dashboard.dto.res.DashboardRecentNotificationResponse;
import s_map.server.domain.dashboard.dto.res.DashboardRiskSummaryResponse;
import s_map.server.domain.dashboard.dto.res.DashboardSummaryResponse;
import s_map.server.domain.dashboard.dto.res.DashboardWeeklyScheduleResponse;
import s_map.server.domain.dashboard.repository.DashboardRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");

    private final DashboardRepository dashboardRepository;

    public DashboardSummaryResponse getSummary() {
        DateRange monthlyRange = getCurrentMonthRange();

        return DashboardSummaryResponse.of(
                monthlyRange.startDate(),
                monthlyRange.endDate(),
                dashboardRepository.countMonthlyProductionTargetOrders(monthlyRange.startAt(), monthlyRange.endExclusive()),
                dashboardRepository.countMonthlyDelayRiskOrders(monthlyRange.startAt(), monthlyRange.endExclusive()),
                dashboardRepository.countMonthlyMaterialTargets(monthlyRange.startAt(), monthlyRange.endExclusive()),
                dashboardRepository.countMonthlyMaterialShortages(monthlyRange.startAt(), monthlyRange.endExclusive()),
                dashboardRepository.countMonthlyDueTargetOrders(monthlyRange.startAt(), monthlyRange.endExclusive()),
                dashboardRepository.countMonthlyOnTimeCompletedOrders(monthlyRange.startAt(), monthlyRange.endExclusive()),
                dashboardRepository.sumMonthlyDelayReductionHours(monthlyRange.startAt(), monthlyRange.endExclusive()),
                OffsetDateTime.now(DEFAULT_PRODUCTION_ZONE)
        );
    }

    public DashboardWeeklyScheduleResponse getWeeklySchedule(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedStartDate = startDate != null
                ? startDate
                : LocalDate.now(DEFAULT_PRODUCTION_ZONE)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        LocalDate resolvedEndDate = endDate != null
                ? endDate
                : resolvedStartDate.plusDays(6);

        OffsetDateTime startAt = resolvedStartDate
                .atStartOfDay(DEFAULT_PRODUCTION_ZONE)
                .toOffsetDateTime();

        OffsetDateTime endExclusive = resolvedEndDate
                .plusDays(1)
                .atStartOfDay(DEFAULT_PRODUCTION_ZONE)
                .toOffsetDateTime();

        return DashboardWeeklyScheduleResponse.of(
                resolvedStartDate,
                resolvedEndDate,
                dashboardRepository.findWeeklySchedules(startAt, endExclusive),
                dashboardRepository.findWeeklyScheduleSegments(startAt, endExclusive)
        );
    }

    public DashboardOrderDeliveryStatusResponse getOrderDeliveryStatus(int limit) {
        int safeLimit = normalizeLimit(limit);
        LocalDate today = LocalDate.now(DEFAULT_PRODUCTION_ZONE);
        OffsetDateTime now = OffsetDateTime.now(DEFAULT_PRODUCTION_ZONE);
        DashboardRepository.OrderDeliveryStatusQueryResult result =
                dashboardRepository.findCurrentOrderDeliveryStatuses(safeLimit, today, now);

        return DashboardOrderDeliveryStatusResponse.from(result.averageProgressRate(), result.orders());
    }

    public DashboardLineUtilizationResponse getLineUtilization() {
        return DashboardLineUtilizationResponse.from(
                dashboardRepository.findLatestLineUtilizations()
        );
    }

    public DashboardRiskSummaryResponse getRiskSummary() {
        DateRange monthlyRange = getCurrentMonthRange();

        return DashboardRiskSummaryResponse.of(
                monthlyRange.startDate(),
                monthlyRange.endDate(),
                dashboardRepository.countMonthlyDelayRiskOrders(monthlyRange.startAt(), monthlyRange.endExclusive()),
                dashboardRepository.countMonthlyMaterialShortages(monthlyRange.startAt(), monthlyRange.endExclusive()),
                dashboardRepository.countCurrentLineRisks(),
                dashboardRepository.countMonthlyRiskLevel(monthlyRange.startAt(), monthlyRange.endExclusive(), "CRITICAL"),
                dashboardRepository.countMonthlyRiskLevel(monthlyRange.startAt(), monthlyRange.endExclusive(), "WARNING"),
                dashboardRepository.findRecentRisks(monthlyRange.startAt(), monthlyRange.endExclusive(), 5)
        );
    }

    public DashboardRecentNotificationResponse getRecentNotifications(int limit) {
        int safeLimit = normalizeLimit(limit);

        return DashboardRecentNotificationResponse.of(
                dashboardRepository.countUnreadNotifications(),
                dashboardRepository.findRecentNotifications(safeLimit)
        );
    }

    private DateRange getCurrentMonthRange() {
        LocalDate today = LocalDate.now(DEFAULT_PRODUCTION_ZONE);
        LocalDate startDate = today.withDayOfMonth(1);

        OffsetDateTime startAt = startDate
                .atStartOfDay(DEFAULT_PRODUCTION_ZONE)
                .toOffsetDateTime();

        OffsetDateTime endExclusive = today
                .plusDays(1)
                .atStartOfDay(DEFAULT_PRODUCTION_ZONE)
                .toOffsetDateTime();

        return new DateRange(startDate, today, startAt, endExclusive);
    }

    private int normalizeLimit(int limit) {
        if (limit < 1) {
            return 5;
        }

        return Math.min(limit, 20);
    }

    private record DateRange(
            LocalDate startDate,
            LocalDate endDate,
            OffsetDateTime startAt,
            OffsetDateTime endExclusive
    ) {
    }
}
