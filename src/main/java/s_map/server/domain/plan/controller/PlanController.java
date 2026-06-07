package s_map.server.domain.plan.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import s_map.server.domain.plan.dto.req.PlanUpdateRequest;
import s_map.server.domain.plan.dto.res.PlanUpdateResponse;
import s_map.server.domain.plan.dto.res.CurrentPlanResponse;
import s_map.server.domain.plan.dto.res.PlanDetailResponse;
import s_map.server.domain.plan.dto.res.PlanListResponse;
import s_map.server.domain.plan.service.PlanService;
import s_map.server.global.common.BaseResponse;

import java.util.List;

@Tag(name = "Plan", description = "생산계획 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    @Operation(
            summary = "생산계획 목록 조회",
            description = "전체 생산계획을 계획 시작 일시 오름차순으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생산계획 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public BaseResponse<List<PlanListResponse>> getPlans() {
        return BaseResponse.success(planService.getPlans());
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
            summary = "생산계획 수정 요청 검증",
            description = "생산계획 변경 요청값의 라인 생산 가능 여부, 담당자, 시간 충돌을 검증합니다. 현재 구현은 실제 DB 반영 없이 검증 결과만 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생산계획 수정 요청 검증 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 일정 충돌"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "생산계획, 담당자 또는 생산 가능 라인 없음"),
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
}
