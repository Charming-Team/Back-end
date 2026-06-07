package s_map.server.domain.plan.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import s_map.server.domain.plan.dto.req.PlanFileApplyMode;
import s_map.server.domain.plan.dto.req.PlanScheduleUpdateRequest;
import s_map.server.domain.plan.dto.req.PlanUpdateRequest;
import s_map.server.domain.plan.dto.res.PlanFileApplyResponse;
import s_map.server.domain.plan.dto.res.PlanFileDownload;
import s_map.server.domain.plan.dto.res.PlanFileValidationResponse;
import s_map.server.domain.plan.dto.res.PlanUpdateResponse;
import s_map.server.domain.plan.dto.res.CurrentPlanResponse;
import s_map.server.domain.plan.dto.res.PlanDetailResponse;
import s_map.server.domain.plan.dto.res.PlanListResponse;
import s_map.server.domain.plan.service.PlanFileService;
import s_map.server.domain.plan.service.PlanService;
import s_map.server.global.common.BaseResponse;

import java.time.OffsetDateTime;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Tag(name = "Plan", description = "생산계획 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;
    private final PlanFileService planFileService;

    @Operation(
            summary = "생산계획 목록 조회",
            description = """
                    생산계획을 상태, 검색어, 기간 조건으로 조회합니다.
                    조건이 없으면 전체 생산계획을 계획 시작 일시 오름차순으로 조회합니다.
                    월간/주간 캘린더 조회는 별도 API가 아니라 startAt, endAt 기간 조건으로 처리합니다.
                    검색어는 계획 ID, 주문 ID, 제품명, 라인명, 라인 코드, 담당자명을 대상으로 합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생산계획 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 검증 실패 또는 지원하지 않는 생산계획 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public BaseResponse<List<PlanListResponse>> getPlans(
            @Parameter(description = "검색어. 계획 ID, 주문 ID, 제품명, 라인, 담당자를 검색합니다.", example = "LINE-A")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "검색어 alias. keyword가 있으면 keyword를 우선합니다.", example = "브레이크")
            @RequestParam(required = false) String search,
            @Parameter(
                    description = "생산계획 상태",
                    example = "SCHEDULED",
                    schema = @Schema(
                            allowableValues = {"SCHEDULED", "IN_PROGRESS", "COMPLETED", "DELAYED", "CANCELLED"}
                    )
            )
            @RequestParam(required = false) String status,
            @Parameter(
                    description = "조회 시작 일시. 계획 종료가 이 값 이후인 일정을 조회합니다. 월간 조회 예: 해당 월 1일 00:00",
                    example = "2026-05-01T00:00:00+09:00"
            )
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @RequestParam(required = false) OffsetDateTime startAt,
            @Parameter(
                    description = "조회 종료 일시. 계획 시작이 이 값보다 이전인 일정을 조회합니다. 월간 조회 예: 다음 월 1일 00:00, 주간 조회 예: 다음 주 시작일 00:00",
                    example = "2026-06-01T00:00:00+09:00"
            )
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @RequestParam(required = false) OffsetDateTime endAt
    ) {
        String resolvedKeyword = keyword != null ? keyword : search;
        return BaseResponse.success(planService.getPlans(resolvedKeyword, status, startAt, endAt));
    }

    @Operation(
            summary = "생산계획 상세 조회",
            description = "생산계획 ID를 기준으로 생산계획 기본 정보와 계획별 필요 자재 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생산계획 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "생산계획 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{planId}")
    public BaseResponse<PlanDetailResponse> getPlan(
            @Parameter(description = "생산계획 ID", example = "1")
            @PathVariable Long planId
    ) {
        return BaseResponse.success(planService.getPlan(planId));
    }

    @Operation(
            summary = "오늘 생산계획 조회",
            description = "Asia/Seoul 기준 오늘 일정에 포함되는 생산계획과 생산실적 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "오늘 생산계획 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/current")
    public BaseResponse<List<CurrentPlanResponse>> getCurrentPlans() {
        return BaseResponse.success(planService.getCurrentPlans());
    }

    @Operation(
            summary = "생산계획 수정",
            description = """
                    생산계획의 라인, 담당자, 계획 시작/종료 일시, 계획 수량, 라인 내 순서, 상태를 수정합니다.
                    계획 ID, 주문 ID, 제품 ID, 생성일시는 수정할 수 없습니다.
                    저장 전 라인 생산 가능 여부, 담당자, 라인 내 순서 중복, 시간 충돌을 검증합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생산계획 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패, 수정 불가능 상태 또는 라인 내 순서 중복"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "생산계획, 담당자 또는 생산 가능 라인 없음"),
            @ApiResponse(responseCode = "409", description = "동일 라인 일정 충돌. 프론트엔드는 AI 분석 안내 창을 표시할 수 있습니다."),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PatchMapping("/{planId}")
    public BaseResponse<PlanUpdateResponse> updatePlan(
            @Parameter(description = "생산계획 ID", example = "1")
            @PathVariable Long planId,
            @Valid @RequestBody PlanUpdateRequest request
    ) {
        return BaseResponse.success(planService.updatePlan(planId, request));
    }

    @Operation(
            summary = "생산계획 일정 이동",
            description = """
                    캘린더 드래그 앤 드롭으로 생산계획의 라인과 계획 시작/종료 일시만 이동합니다.
                    lineId를 전달하지 않으면 기존 라인을 유지합니다.
                    이동 위치에 동일 라인의 다른 생산계획이 겹치면 저장하지 않고 409-601 충돌 응답을 반환합니다.
                    프론트엔드는 이 충돌 응답을 기준으로 AI 분석 안내 창을 표시합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생산계획 일정 이동 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 수정 불가능 상태"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "생산계획 또는 생산 가능 라인 없음"),
            @ApiResponse(responseCode = "409", description = "동일 라인 일정 충돌. AI 분석 안내 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PatchMapping("/{planId}/schedule")
    public BaseResponse<PlanUpdateResponse> movePlanSchedule(
            @Parameter(description = "생산계획 ID", example = "1")
            @PathVariable Long planId,
            @Valid @RequestBody PlanScheduleUpdateRequest request
    ) {
        return BaseResponse.success(planService.movePlanSchedule(planId, request));
    }

    @Operation(
            summary = "생산계획 파일 Export",
            description = """
                    현재 생산계획을 CSV 또는 XLSX 파일로 다운로드합니다.
                    업로드 반영 시 plan_id, 제품/라인/담당자명, estimated_duration_hr, plan_status는 참고용이며 사용자 입력값으로 반영하지 않습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생산계획 파일 다운로드 성공"),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 파일 형식"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/files/export")
    public ResponseEntity<byte[]> exportPlanFile(
            @Parameter(description = "파일 형식. csv 또는 xlsx", example = "csv")
            @RequestParam(defaultValue = "csv") String format
    ) {
        PlanFileDownload file = planFileService.exportPlans(format);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(file.fileName(), StandardCharsets.UTF_8)
                                .build()
                                .toString()
                )
                .body(file.content());
    }

    @Operation(
            summary = "생산계획 파일 검증",
            description = """
                    CSV 또는 XLSX 생산계획 파일의 컬럼, 필수값, 주문/제품/라인/담당자 매칭, 일정 충돌을 검증합니다.
                    mode는 INITIAL_REGISTER 또는 FULL_REPLACE 중 하나입니다.
                    이 API는 DB에 반영하지 않고 검증 결과만 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생산계획 파일 검증 완료"),
            @ApiResponse(responseCode = "400", description = "파일 형식 또는 컬럼 구성 오류"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping(value = "/files/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<PlanFileValidationResponse> validatePlanFile(
            @Parameter(description = "반영 방식", example = "FULL_REPLACE")
            @RequestParam PlanFileApplyMode mode,
            @Parameter(description = "CSV 또는 XLSX 파일")
            @RequestParam MultipartFile file
    ) {
        return BaseResponse.success(planFileService.validatePlanFile(file, mode));
    }

    @Operation(
            summary = "생산계획 파일 반영",
            description = """
                    CSV 또는 XLSX 생산계획 파일을 검증한 뒤 production_plans에 반영합니다.
                    검증 오류가 있으면 DB에 반영하지 않고 applied=false 응답을 반환합니다.
                    INITIAL_REGISTER는 현재 운영 생산계획이 없을 때 신규 등록합니다.
                    FULL_REPLACE는 현재 운영 생산계획을 CANCELLED로 변경한 뒤 업로드 파일 기준 신규 계획을 생성합니다.
                    estimated_duration_hr와 plan_status 파일 값은 반영하지 않고, 예상 소요 시간은 기준 데이터로 계산하며 상태는 SCHEDULED로 등록합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생산계획 파일 반영 처리 완료"),
            @ApiResponse(responseCode = "400", description = "파일 형식 또는 컬럼 구성 오류"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping(value = "/files/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BaseResponse<PlanFileApplyResponse> applyPlanFile(
            @Parameter(description = "반영 방식", example = "FULL_REPLACE")
            @RequestParam PlanFileApplyMode mode,
            @Parameter(description = "CSV 또는 XLSX 파일")
            @RequestParam MultipartFile file
    ) {
        return BaseResponse.success(planFileService.applyPlanFile(file, mode));
    }
}
