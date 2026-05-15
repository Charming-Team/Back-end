package s_map.server.domain.plan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.material.entity.ProductionPlanMaterial;
import s_map.server.domain.material.repository.ProductionPlanMaterialRepository;
import s_map.server.domain.plan.dto.res.CurrentPlanResponse;
import s_map.server.domain.plan.dto.res.PlanDetailResponse;
import s_map.server.domain.plan.dto.res.PlanListResponse;
import s_map.server.domain.plan.entity.ProductionPlan;
import s_map.server.domain.plan.entity.ProductionResult;
import s_map.server.domain.plan.repository.ProductionPlanRepository;
import s_map.server.domain.plan.repository.ProductionResultRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanService {

    private final ProductionPlanRepository productionPlanRepository;
    private final ProductionResultRepository productionResultRepository;

    /*
     * ProductionPlanMaterial은 현재 material 도메인에 위치함.
     * 생산계획 상세 조회 시 해당 계획에 필요한 자재 목록을 조회하기 위해 참조한다.
     */
    private final ProductionPlanMaterialRepository productionPlanMaterialRepository;

    /**
     * [기능]
     * 전체 생산계획 목록을 조회한다.
     *
     * [Input]
     * - 없음
     *
     * [Process]
     * - production_plans 테이블에서 전체 생산계획을 조회한다.
     * - 계획 시작 시간(plannedStartAt) 기준 오름차순으로 정렬한다.
     * - Entity 목록을 프론트엔드 응답용 DTO 목록으로 변환한다.
     *
     * [Output]
     * - List<PlanListResponse>
     * - 생산계획 ID, 주문 ID, 제품 ID, 라인 ID, 담당자 ID,
     *   계획 시작/종료 시간, 예상 소요 시간, 계획 수량, 계획 순서, 계획 상태를 반환한다.
     */
    public List<PlanListResponse> getPlans() {
        return productionPlanRepository.findAllByOrderByPlannedStartAtAsc()
                .stream()
                .map(PlanListResponse::from)
                .toList();
    }

    /**
     * [기능]
     * 특정 생산계획의 상세 정보를 조회한다.
     *
     * [Input]
     * - planId: 조회할 생산계획 ID
     *
     * [Process]
     * - planId로 production_plans 테이블에서 생산계획을 조회한다.
     * - 생산계획이 존재하지 않으면 PRODUCTION_PLAN_NOT_FOUND 예외를 발생시킨다.
     * - production_plan_materials 테이블에서 해당 생산계획에 필요한 자재 목록을 조회한다.
     * - 생산계획 기본 정보와 자재 소요/예약/부족 정보를 하나의 DTO로 조합한다.
     *
     * [Output]
     * - PlanDetailResponse
     * - 생산계획 기본 정보와 계획별 필요 자재 목록을 반환한다.
     */
    public PlanDetailResponse getPlan(Long planId) {
        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCTION_PLAN_NOT_FOUND));

        List<ProductionPlanMaterial> planMaterials =
                productionPlanMaterialRepository.findByPlanId(planId);

        return PlanDetailResponse.of(plan, planMaterials);
    }

    /**
     * [기능]
     * 현재 날짜 기준으로 오늘 일정에 포함되는 생산계획 목록을 조회한다.
     *
     * [Input]
     * - 없음
     *
     * [Process]
     * - 오늘 날짜의 시작 시각과 종료 시각을 계산한다.
     * - plannedStartAt <= 오늘 종료 시각 AND plannedEndAt >= 오늘 시작 시각 조건으로
     *   오늘 일정에 걸쳐 있는 생산계획을 조회한다.
     * - 조회된 생산계획 ID 목록으로 production_results 테이블에서 실적 정보를 조회한다.
     * - 생산계획과 생산실적을 planId 기준으로 매칭한다.
     * - 실적이 없는 생산계획은 실제 수량/불량 수량을 0으로 내려준다.
     *
     * [Output]
     * - List<CurrentPlanResponse>
     * - 현재 생산계획 정보와 실제 생산 실적 정보를 함께 반환한다.
     */
    public List<CurrentPlanResponse> getCurrentPlans() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<ProductionPlan> plans =
                productionPlanRepository
                        .findByPlannedStartAtLessThanEqualAndPlannedEndAtGreaterThanEqualOrderByPlannedStartAtAsc(
                                endOfDay,
                                startOfDay
                        );

        List<Long> planIds = plans.stream()
                .map(ProductionPlan::getPlanId)
                .toList();

        Map<Long, ProductionResult> resultMap =
                productionResultRepository.findByPlanIdIn(planIds)
                        .stream()
                        .collect(Collectors.toMap(
                                ProductionResult::getPlanId,
                                Function.identity(),
                                (first, second) -> first
                        ));

        return plans.stream()
                .map(plan -> CurrentPlanResponse.of(
                        plan,
                        resultMap.get(plan.getPlanId())
                ))
                .toList();
    }
}