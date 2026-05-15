package s_map.server.domain.plan.controller;

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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    @GetMapping
    public BaseResponse<List<PlanListResponse>> getPlans() {
        return BaseResponse.success(planService.getPlans());
    }

    @GetMapping("/{planId}")
    public BaseResponse<PlanDetailResponse> getPlan(
            @PathVariable Long planId
    ) {
        return BaseResponse.success(planService.getPlan(planId));
    }

    @GetMapping("/current")
    public BaseResponse<List<CurrentPlanResponse>> getCurrentPlans() {
        return BaseResponse.success(planService.getCurrentPlans());
    }

    @PatchMapping("/{planId}")
    public BaseResponse<PlanUpdateResponse> updatePlan(
            @PathVariable Long planId,
            @RequestBody PlanUpdateRequest request
    ) {
        return BaseResponse.success(planService.updatePlan(planId, request));
    }
}