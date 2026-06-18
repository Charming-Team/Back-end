package s_map.server.domain.plan.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.plan.dto.res.PlanSimulationDetailResponse;
import s_map.server.domain.plan.dto.res.PlanSimulationListResponse;
import s_map.server.domain.plan.service.PlanSimulationService;
import s_map.server.global.common.BaseResponse;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import s_map.server.domain.plan.dto.req.PlanSimulationSelectedSaveRequest;
import s_map.server.domain.plan.dto.res.PlanSimulationSelectedSaveResponse;
import s_map.server.global.security.AuthUser;

import java.util.List;

@Tag(name = "Plan Simulation", description = "생산계획 시뮬레이션 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans/simulations")
public class PlanSimulationController {

    private final PlanSimulationService planSimulationService;

    @Operation(
            summary = "생산계획 시뮬레이션 결과 목록 조회",
            description = "생산계획 시뮬레이션 실행 결과를 생성 일시 내림차순으로 조회합니다. 병목 라인, 지연 감소 시간, 라인 평균 가동률, 생산량 변화, 적용 여부를 함께 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생산계획 시뮬레이션 결과 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public BaseResponse<List<PlanSimulationListResponse>> getSimulations() {
        return BaseResponse.success(planSimulationService.getSimulations());
    }

    @Operation(
            summary = "생산계획 시뮬레이션 상세 변경 내역 조회",
            description = "시뮬레이션 ID를 기준으로 생산계획별 변경 전후 라인, 순서, 일정, 수량, 지연 여부와 변경 사유를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생산계획 시뮬레이션 상세 변경 내역 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "시뮬레이션 결과 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/{simulationId}/details")
    public BaseResponse<List<PlanSimulationDetailResponse>> getSimulationDetails(
            @Parameter(description = "시뮬레이션 결과 ID", example = "1")
            @PathVariable Long simulationId
    ) {
        return BaseResponse.success(planSimulationService.getSimulationDetails(simulationId));
    }

    @Operation(
            summary = "사용자 선택 생산계획 시뮬레이션 저장",
            description = """
                FastAPI 생산계획 조정 결과 중 사용자가 선택한 대안 1개만 저장합니다.
                AI 전체 결과는 저장하지 않습니다.
                선택된 plans는 production_plans에 저장하고,
                시뮬레이션 요약은 schedule_simulation_results에,
                상세 변경 내역은 schedule_simulation_details에 저장합니다.
                """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "선택 생산계획 시뮬레이션 저장 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 검증 실패 또는 반영 불가능한 선택안"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "생산계획 반영 권한 필요"),
            @ApiResponse(responseCode = "404", description = "생산계획, 주문 또는 시뮬레이션 결과 없음"),
            @ApiResponse(responseCode = "409", description = "동일 라인 일정 또는 순서 충돌"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/selected")
    public BaseResponse<PlanSimulationSelectedSaveResponse> saveSelectedSimulation(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody PlanSimulationSelectedSaveRequest request
    ) {
        return BaseResponse.success(
                planSimulationService.saveSelectedSimulation(
                        request,
                        authUser.id()
                )
        );
    }
}
