package s_map.server.domain.dashboard.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Dashboard", description = "공장 대시보드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(
            summary = "대시보드 상단 KPI 요약 조회",
            description = "현재 대시보드 기준 기간의 지연 위험 주문 수, 자재 부족 품목 수, 주문별 달성률, 생산계획 절약 시간을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대시보드 KPI 요약 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/summary")
    public BaseResponse<DashboardSummaryResponse> getSummary() {
        return BaseResponse.success(dashboardService.getSummary());
    }

    @Operation(
            summary = "주간 생산 스케줄 조회",
            description = "지정한 기간의 생산계획과 라인 상태 구간을 라인별로 조회합니다. 날짜를 생략하면 이번 주 월요일부터 7일간 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주간 생산 스케줄 조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/weekly-schedule")
    public BaseResponse<DashboardWeeklyScheduleResponse> getWeeklySchedule(
            @Parameter(description = "조회 시작일. 생략 시 이번 주 월요일", example = "2026-06-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Parameter(description = "조회 종료일. 생략 시 시작일 기준 6일 뒤", example = "2026-06-07")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        return BaseResponse.success(dashboardService.getWeeklySchedule(startDate, endDate));
    }

    @Operation(
            summary = "주문 및 납기 현황 조회",
            description = "현재 진행 중이거나 지연 상태인 주문의 납기일, 진행률, 상태와 전체 평균 진행률을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 및 납기 현황 조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/order-delivery-status")
    public BaseResponse<DashboardOrderDeliveryStatusResponse> getOrderDeliveryStatus(
            @Parameter(description = "조회할 주문 최대 개수. 서버에서 최대 20개로 제한", example = "5")
            @RequestParam(defaultValue = "5") int limit
    ) {
        return BaseResponse.success(dashboardService.getOrderDeliveryStatus(limit));
    }

    @Operation(
            summary = "라인별 가동 현황 조회",
            description = "활성 생산 라인의 최신 가동률과 표시 상태를 조회합니다. 상태 데이터가 없으면 상태 확인 필요로 표시합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "라인별 가동 현황 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/line-utilization")
    public BaseResponse<DashboardLineUtilizationResponse> getLineUtilization() {
        return BaseResponse.success(dashboardService.getLineUtilization());
    }

    @Operation(
            summary = "리스크 요약 조회",
            description = "대시보드 기준 기간의 지연 위험 주문, 자재 리스크, 라인 리스크, 위험 레벨별 건수와 최근 위험 주문을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "리스크 요약 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/risk-summary")
    public BaseResponse<DashboardRiskSummaryResponse> getRiskSummary() {
        return BaseResponse.success(dashboardService.getRiskSummary());
    }

    @Operation(
            summary = "최근 알림 조회",
            description = "대시보드 우측 알림 영역에 표시할 최근 알림 목록과 미읽음 알림 수를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "최근 알림 조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/recent-notifications")
    public BaseResponse<DashboardRecentNotificationResponse> getRecentNotifications(
            @Parameter(description = "조회할 알림 최대 개수. 서버에서 최대 20개로 제한", example = "5")
            @RequestParam(defaultValue = "5") int limit
    ) {
        return BaseResponse.success(dashboardService.getRecentNotifications(limit));
    }
}
