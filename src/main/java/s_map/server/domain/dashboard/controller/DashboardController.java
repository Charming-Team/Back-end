package s_map.server.domain.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.dashboard.dto.res.DashboardLineUtilizationResponse;
import s_map.server.domain.dashboard.dto.res.DashboardOrderDeliveryStatusResponse;
import s_map.server.domain.dashboard.dto.res.DashboardRecentNotificationResponse;
import s_map.server.domain.dashboard.dto.res.DashboardRiskSummaryResponse;
import s_map.server.domain.dashboard.dto.res.DashboardSummaryResponse;
import s_map.server.domain.dashboard.dto.res.DashboardWeeklyScheduleResponse;
import s_map.server.domain.dashboard.service.DashboardService;
import s_map.server.global.common.BaseResponse;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public BaseResponse<DashboardSummaryResponse> getSummary() {
        return BaseResponse.success(dashboardService.getSummary());
    }

    @GetMapping("/weekly-schedule")
    public BaseResponse<DashboardWeeklyScheduleResponse> getWeeklySchedule(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return BaseResponse.success(dashboardService.getWeeklySchedule(startDate, endDate));
    }

    @GetMapping("/order-delivery-status")
    public BaseResponse<DashboardOrderDeliveryStatusResponse> getOrderDeliveryStatus(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return BaseResponse.success(dashboardService.getOrderDeliveryStatus(limit));
    }

    @GetMapping("/line-utilization")
    public BaseResponse<DashboardLineUtilizationResponse> getLineUtilization() {
        return BaseResponse.success(dashboardService.getLineUtilization());
    }

    @GetMapping("/risk-summary")
    public BaseResponse<DashboardRiskSummaryResponse> getRiskSummary() {
        return BaseResponse.success(dashboardService.getRiskSummary());
    }

    @GetMapping("/recent-notifications")
    public BaseResponse<DashboardRecentNotificationResponse> getRecentNotifications(
            @RequestParam(defaultValue = "5") int limit
    ) {
        return BaseResponse.success(dashboardService.getRecentNotifications(limit));
    }
}