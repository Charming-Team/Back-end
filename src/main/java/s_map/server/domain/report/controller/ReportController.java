package s_map.server.domain.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.report.dto.req.BusinessReportGenerateRequest;
import s_map.server.domain.report.dto.req.ReportGenerateRequest;
import s_map.server.domain.report.dto.res.ReportDetailResponse;
import s_map.server.domain.report.dto.res.ReportGenerateStartResponse;
import s_map.server.domain.report.dto.res.ReportJobResponse;
import s_map.server.domain.report.dto.res.ReportListResponse;
import s_map.server.domain.report.service.ReportService;
import s_map.server.global.common.BaseResponse;
import s_map.server.global.security.AuthUser;

@Tag(name = "Report", description = "보고서 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    @Operation(
            summary = "보고서 생성 요청",
            description = "인증 사용자의 보고서 생성 요청을 접수하고 비동기 생성 Job을 시작합니다. 응답의 reportJobId로 생성 상태를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보고서 생성 작업 접수 성공"),
            @ApiResponse(responseCode = "400", description = "보고서 요청 값 또는 기간 조건 오류"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "보고서 접근 권한 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/generate")
    public BaseResponse<ReportGenerateStartResponse> generateReport(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser,
            @RequestBody ReportGenerateRequest request
    ) {
        return BaseResponse.success(reportService.generateReport(authUser, request));
    }

    @Operation(
            summary = "비즈니스 보고서 생성 요청",
            description = "기존 보고서 ID를 기준으로 경영진용 비즈니스 보고서 생성 Job을 시작합니다. 응답의 reportJobId로 생성 상태를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비즈니스 보고서 생성 작업 접수 성공"),
            @ApiResponse(responseCode = "400", description = "비즈니스 보고서 요청 값 오류"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "보고서 접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "원본 보고서 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/business")
    public BaseResponse<ReportGenerateStartResponse> generateBusinessReport(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody BusinessReportGenerateRequest request
    ) {
        return BaseResponse.success(reportService.generateBusinessReport(authUser, request));
    }

    @Operation(
            summary = "보고서 생성 Job 조회",
            description = "보고서 생성 Job ID를 기준으로 현재 상태, 실패 사유, 생성된 보고서 ID를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보고서 생성 Job 조회 성공"),
            @ApiResponse(responseCode = "400", description = "보고서 생성 Job ID 오류"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "보고서 접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "보고서 생성 Job 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/jobs/{reportJobId}")
    public BaseResponse<ReportJobResponse> getReportJob(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "보고서 생성 Job ID", example = "1")
            @PathVariable Long reportJobId
    ) {
        return BaseResponse.success(reportService.getReportJob(authUser, reportJobId));
    }

    @Operation(
            summary = "보고서 목록 조회",
            description = "보고서 목록을 작성일시 기준 최신순으로 페이지 단위 조회합니다. 작성자 이름은 authorName으로 함께 내려갑니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보고서 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "보고서 접근 권한 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public BaseResponse<Page<ReportListResponse>> getReports(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기, 최대 100", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        return BaseResponse.success(reportService.getReports(authUser, page, size));
    }

    @Operation(
            summary = "보고서 상세 조회",
            description = "보고서 ID를 기준으로 보고서 기본 정보, 본문 Markdown, 섹션 데이터, 근거 데이터를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보고서 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "보고서 ID 오류"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "보고서 접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "보고서 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{reportId}")
    public BaseResponse<ReportDetailResponse> getReport(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "보고서 ID", example = "1")
            @PathVariable Long reportId
    ) {
        return BaseResponse.success(reportService.getReport(authUser, reportId));
    }
}
