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

    /**
     * 기능: 공장 대시보드 상단 요약 카드에 필요한 월간 KPI를 조회한다.
     *
     * Input:
     * - 없음
     *
     * Output:
     * - result / DashboardSummaryResponse / 지연 위험 주문, 자재 부족, 주문 달성률, 생산계획 절약 시간 요약
     */
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

    /**
     * 기능: 주간 기준 생산 스케줄을 라인별로 조회한다.
     *
     * Input:
     * - startDate / LocalDate / 조회 시작일, 없으면 이번 주 월요일
     * - endDate / LocalDate / 조회 종료일, 없으면 시작일 기준 6일 뒤
     *
     * Output:
     * - result / DashboardWeeklyScheduleResponse / 라인별 주간 계획 목록과 상태 구간 목록
     */
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

    /**
     * 기능: 대시보드 주문 및 납기 현황 영역에 표시할 주문별 진행률과 상태를 조회한다.
     *
     * Input:
     * - limit / int / 조회할 주문 개수
     *
     * Output:
     * - result / DashboardOrderDeliveryStatusResponse / 주문별 납기일, 진행률, 상태, 전체 평균 진행률
     */
    public DashboardOrderDeliveryStatusResponse getOrderDeliveryStatus(int limit) {
        int safeLimit = normalizeLimit(limit);
        LocalDate today = LocalDate.now(DEFAULT_PRODUCTION_ZONE);
        OffsetDateTime now = OffsetDateTime.now(DEFAULT_PRODUCTION_ZONE);
        DashboardRepository.OrderDeliveryStatusQueryResult result =
                dashboardRepository.findCurrentOrderDeliveryStatuses(safeLimit, today, now);

        return DashboardOrderDeliveryStatusResponse.from(result.averageProgressRate(), result.orders());
    }

    /**
     * 기능: 대시보드 라인별 가동 현황 영역에 표시할 최신 가동률과 상태를 조회한다.
     *
     * Input:
     * - 없음
     *
     * Output:
     * - result / DashboardLineUtilizationResponse / 라인별 최신 가동률과 가동 상태 목록
     */
    public DashboardLineUtilizationResponse getLineUtilization() {
        return DashboardLineUtilizationResponse.from(
                dashboardRepository.findLatestLineUtilizations()
        );
    }

    /**
     * 기능: 대시보드 리스크 요약 영역에 필요한 월간 리스크 집계와 최근 리스크 목록을 조회한다.
     *
     * Input:
     * - 없음
     *
     * Output:
     * - result / DashboardRiskSummaryResponse / 지연, 자재 부족, 라인 리스크, 심각도별 집계, 최근 리스크 목록
     */
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

    /**
     * 기능: 대시보드 알림 영역에 표시할 읽지 않은 알림 수와 최근 알림 목록을 조회한다.
     *
     * Input:
     * - limit / int / 조회할 알림 개수
     *
     * Output:
     * - result / DashboardRecentNotificationResponse / 읽지 않은 알림 수와 최근 알림 목록
     */
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
