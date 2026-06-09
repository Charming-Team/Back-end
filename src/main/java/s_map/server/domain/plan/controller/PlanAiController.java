package s_map.server.domain.plan.controller;

import io.swagger.v3.oas.annotations.Operation;
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
}
