package s_map.server.domain.report.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.report.dto.req.BusinessReportGenerateRequest;
import s_map.server.domain.report.dto.req.ReportGenerateRequest;
import s_map.server.domain.report.dto.req.ReportUpdateRequest;
import s_map.server.domain.report.dto.res.ReportDetailResponse;
import s_map.server.domain.report.dto.res.ReportGenerateStartResponse;
import s_map.server.domain.report.dto.res.ReportJobResponse;
import s_map.server.domain.report.dto.res.ReportListResponse;
import s_map.server.domain.report.dto.res.ReportPdfDownloadResponse;
import s_map.server.domain.report.service.ReportService;
import s_map.server.global.common.BaseResponse;
import s_map.server.global.common.PageResponse;
import s_map.server.global.security.AuthUser;

import java.nio.charset.StandardCharsets;

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
    public BaseResponse<PageResponse<ReportListResponse>> getReports(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "페이지 번호, 0부터 시작", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기, 최대 100", example = "10")
            @RequestParam(defaultValue = "10") int size
    ) {
        return BaseResponse.success(PageResponse.from(reportService.getReports(authUser, page, size)));
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

    @Operation(
            summary = "보고서 PDF 다운로드",
            description = "보고서 상세 화면의 최신 저장 버전을 기준으로 PDF 파일을 생성하고 다운로드합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "보고서 PDF 다운로드 성공",
                    headers = {
                            @Header(
                                    name = "Content-Disposition",
                                    description = "attachment; filename*=UTF-8''{fileName}.pdf"
                            )
                    },
                    content = @Content(
                            mediaType = MediaType.APPLICATION_PDF_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "보고서 ID 오류"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "PDF 다운로드 권한 없음"),
            @ApiResponse(responseCode = "404", description = "출력할 보고서 없음"),
            @ApiResponse(responseCode = "500", description = "PDF 생성 실패")
    })
    @GetMapping(value = "/{reportId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadReportPdf(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "보고서 ID", example = "1")
            @PathVariable Long reportId
    ) {
        ReportPdfDownloadResponse response = reportService.downloadReportPdf(authUser, reportId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(response.contentLength());
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(response.fileName(), StandardCharsets.UTF_8)
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(response.content());
    }

    @Operation(
            summary = "보고서 내용 수정",
            description = "보고서 제목, Markdown 본문, 상세 화면 구조화 데이터를 수정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보고서 내용 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "보고서 접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "보고서 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PatchMapping("/{reportId}")
    public BaseResponse<ReportDetailResponse> updateReport(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "보고서 ID", example = "1")
            @PathVariable Long reportId,
            @Valid @RequestBody ReportUpdateRequest request
    ) {
        return BaseResponse.success(reportService.updateReport(authUser, reportId, request));
    }

    @Operation(
            summary = "보고서 상세 화면 구조화 데이터 보정",
            description = "기존 보고서에 상세 화면용 구조화 데이터가 없을 때 현재 DB 집계 기준으로 생성하여 저장합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "보고서 구조화 데이터 보정 성공"),
            @ApiResponse(responseCode = "400", description = "보고서 ID 오류"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "보고서 접근 권한 없음"),
            @ApiResponse(responseCode = "404", description = "보고서 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/{reportId}/structured-data/backfill")
    public BaseResponse<ReportDetailResponse> backfillReportStructuredData(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "보고서 ID", example = "1")
            @PathVariable Long reportId
    ) {
        return BaseResponse.success(reportService.backfillReportStructuredData(authUser, reportId));
    }
}
