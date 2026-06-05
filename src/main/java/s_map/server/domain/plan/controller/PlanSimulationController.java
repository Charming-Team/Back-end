package s_map.server.domain.plan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s_map.server.domain.plan.dto.res.PlanSimulationDetailResponse;
import s_map.server.domain.plan.dto.res.PlanSimulationListResponse;
import s_map.server.domain.plan.service.PlanSimulationService;
import s_map.server.global.common.BaseResponse;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans/simulations")
public class PlanSimulationController {

    private final PlanSimulationService planSimulationService;

    @GetMapping
    public BaseResponse<List<PlanSimulationListResponse>> getSimulations() {
        return BaseResponse.success(planSimulationService.getSimulations());
    }

    @GetMapping("/{simulationId}/details")
    public BaseResponse<List<PlanSimulationDetailResponse>> getSimulationDetails(
            @PathVariable Long simulationId
    ) {
        return BaseResponse.success(planSimulationService.getSimulationDetails(simulationId));
    }
}