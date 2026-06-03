package s_map.server.domain.report.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.report.dto.req.ReportGenerateRequest;
import s_map.server.domain.report.dto.res.ReportDetailResponse;
import s_map.server.domain.report.dto.res.ReportGenerateStartResponse;
import s_map.server.domain.report.dto.res.ReportJobResponse;
import s_map.server.domain.report.dto.res.ReportListResponse;
import s_map.server.domain.report.service.ReportService;
import s_map.server.global.common.BaseResponse;
import s_map.server.global.security.AuthUser;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/generate")
    public BaseResponse<ReportGenerateStartResponse> generateReport(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody ReportGenerateRequest request
    ) {
        return BaseResponse.success(reportService.generateReport(authUser, request));
    }

    @GetMapping("/jobs/{reportJobId}")
    public BaseResponse<ReportJobResponse> getReportJob(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reportJobId
    ) {
        return BaseResponse.success(reportService.getReportJob(authUser, reportJobId));
    }

    @GetMapping
    public BaseResponse<List<ReportListResponse>> getReports(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return BaseResponse.success(reportService.getReports(authUser));
    }

    @GetMapping("/{reportId}")
    public BaseResponse<ReportDetailResponse> getReport(
            @AuthenticationPrincipal AuthUser authUser,
            @PathVariable Long reportId
    ) {
        return BaseResponse.success(reportService.getReport(authUser, reportId));
    }
}
