package s_map.server.domain.plan.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.plan.dto.req.PlanAiGenerateRequest;
import s_map.server.domain.plan.dto.req.PlanAiMonthlyGenerateRequest;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateResponse;
import s_map.server.domain.plan.service.PlanAiService;
import s_map.server.global.common.BaseResponse;

@Tag(name = "Plan AI", description = "생산계획 AI 연동 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans/ai")
public class PlanAiController {

    private final PlanAiService planAiService;

    @Operation(
            summary = "AI 생산계획 조정 결과 생성",
            description = """
                    FastAPI 생산계획 조정 API를 호출합니다.
                    프론트는 충돌난 생산계획 ID와 이동 목표 라인/시간, 재계획 기간만 전달합니다.
                    Spring이 생산계획/주문 정보를 DB에서 조회해 FastAPI 요청 body로 변환합니다.
                    AI 응답은 DB에 저장하지 않고 그대로 프론트에 반환합니다.
                    사용자가 특정 시뮬레이션 후보를 선택하면 별도 저장 API에서 DB에 저장합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI 생산계획 조정 결과 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "생산계획 또는 주문 없음"),
            @ApiResponse(responseCode = "500", description = "AI 서버 호출 실패 또는 서버 내부 오류")
    })
    @PostMapping("/generate")
    public BaseResponse<FastApiPlanningGenerateResponse> generatePlanning(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @Valid @RequestBody PlanAiGenerateRequest request
    ) {
        return BaseResponse.success(
                planAiService.generatePlanning(
                        request,
                        authorizationHeader,
                        refreshToken
                )
        );
    }

    @Operation(
            summary = "월간 AI 생산계획 분석",
            description = """
                    현재 캘린더 월 범위의 생산계획을 기준으로 FastAPI 월간 재계획/시뮬레이션 API를 호출합니다.
                    특정 드래그 앤 드롭 충돌을 해결하는 API가 아니라, 조회 중인 월 전체 운영 계획을 대상으로 분석합니다.
                    Spring은 edit_orders/add_orders를 비운 채 planning_start/planning_end만 전달하고,
                    FastAPI는 해당 기간의 DB 생산계획을 기준으로 월간 대안을 생성합니다.
                    AI 응답은 DB에 저장하지 않고 그대로 프론트에 반환합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "월간 AI 생산계획 분석 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "AI 서버 호출 실패 또는 서버 내부 오류")
    })
    @PostMapping("/monthly-analysis")
    public BaseResponse<FastApiPlanningGenerateResponse> generateMonthlyPlanning(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @Valid @RequestBody PlanAiMonthlyGenerateRequest request
    ) {
        return BaseResponse.success(
                planAiService.generateMonthlyPlanning(
                        request,
                        authorizationHeader,
                        refreshToken
                )
        );
    }
}
