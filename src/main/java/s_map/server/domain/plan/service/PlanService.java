package s_map.server.domain.plan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.material.entity.ProductionPlanMaterial;
import s_map.server.domain.material.repository.ProductionPlanMaterialRepository;
import s_map.server.domain.plan.dto.req.PlanUpdateRequest;
import s_map.server.domain.plan.dto.res.CurrentPlanResponse;
import s_map.server.domain.plan.dto.res.PlanDetailResponse;
import s_map.server.domain.plan.dto.res.PlanListResponse;
import s_map.server.domain.plan.dto.res.PlanUpdateResponse;
import s_map.server.domain.plan.entity.PlanStatus;
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
                productionPlanMaterialRepository.findByPlanIdWithMaterial(planId);

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

    /**
     * [기능]
     * 특정 생산계획 정보를 수정한다.
     *
     * [Input]
     * - planId: 수정할 생산계획 ID
     * - request: 수정할 생산계획 정보
     *   - lineId: 변경할 생산라인 ID
     *   - operatorId: 변경할 담당자 ID
     *   - plannedStartAt: 계획 시작 시각
     *   - plannedEndAt: 계획 종료 시각
     *   - plannedQuantity: 계획 생산 수량
     *   - planSequence: 라인 내 생산 순서
     *   - planStatus: 생산계획 상태
     *
     * [Process]
     * - planId로 생산계획을 조회한다.
     * - 생산계획이 없으면 PRODUCTION_PLAN_NOT_FOUND 예외를 발생시킨다.
     * - 완료 또는 취소 상태의 생산계획이면 수정할 수 없도록 차단한다.
     * - 요청값의 필수값, 기간, 수량, 순서를 검증한다.
     * - 동일 라인에 겹치는 일정이 있는지 검증한다.
     * - Entity의 updatePlan 메서드를 통해 수정 가능한 항목만 변경한다.
     * - estimatedDurationHr는 사용자가 직접 수정하지 않는다.
     * - @Transactional에 의해 변경 감지로 DB에 반영된다.
     *
     * [Output]
     * - PlanUpdateResponse
     * - 수정된 생산계획 정보를 반환한다.
     */
    @Transactional
    public PlanUpdateResponse updatePlan(Long planId, PlanUpdateRequest request) {
        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCTION_PLAN_NOT_FOUND));

        validateEditablePlan(plan);
        validateUpdateRequest(request);
        validateScheduleConflict(planId, request);

        plan.updatePlan(
                request.getLineId(),
                request.getOperatorId(),
                request.getPlannedStartAt(),
                request.getPlannedEndAt(),
                request.getPlannedQuantity(),
                request.getPlanSequence(),
                request.getPlanStatus()
        );

        return PlanUpdateResponse.from(plan);
    }

    /**
     * [기능]
     * 수정 가능한 생산계획 상태인지 검증한다.
     *
     * [Input]
     * - plan: 수정 대상 생산계획 Entity
     *
     * [Process]
     * - 생산 완료(COMPLETED) 상태인지 확인한다.
     * - 취소(CANCELLED) 상태인지 확인한다.
     * - 완료 또는 취소 상태라면 수정 요청을 차단한다.
     *
     * [Output]
     * - 없음
     * - 수정 불가능한 상태이면 BAD_REQUEST 예외를 발생시킨다.
     */
    private void validateEditablePlan(ProductionPlan plan) {
        if (plan.getPlanStatus() == PlanStatus.COMPLETED
                || plan.getPlanStatus() == PlanStatus.CANCELLED) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    /**
     * [기능]
     * 생산계획 수정 요청 값의 유효성을 검증한다.
     *
     * [Input]
     * - request: 생산계획 수정 요청 DTO
     *
     * [Process]
     * - 필수 값 누락 여부를 확인한다.
     * - 계획 시작 시각이 종료 시각보다 이전인지 확인한다.
     * - 계획 수량이 1 이상인지 확인한다.
     * - 라인 내 순서가 1 이상인지 확인한다.
     * - 계획 상태 값이 존재하는지 확인한다.
     *
     * [Output]
     * - 없음
     * - 검증 실패 시 BAD_REQUEST 예외를 발생시킨다.
     */
    private void validateUpdateRequest(PlanUpdateRequest request) {
        if (request.getLineId() == null
                || request.getPlannedStartAt() == null
                || request.getPlannedEndAt() == null
                || request.getPlannedQuantity() == null
                || request.getPlanSequence() == null
                || request.getPlanStatus() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        if (!request.getPlannedStartAt().isBefore(request.getPlannedEndAt())) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        if (request.getPlannedQuantity() <= 0) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        if (request.getPlanSequence() < 1) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    /**
     * [기능]
     * 동일 라인 내 생산계획 일정 충돌 여부를 검증한다.
     *
     * [Input]
     * - planId: 현재 수정 중인 생산계획 ID
     * - request: 수정 요청 DTO
     *
     * [Process]
     * - 같은 lineId를 가진 다른 생산계획 중 시간이 겹치는 일정이 있는지 조회한다.
     * - 현재 수정 중인 planId는 충돌 검증 대상에서 제외한다.
     * - 기존 계획 시작 시간이 수정 종료 시각보다 이전이고,
     *   기존 계획 종료 시간이 수정 시작 시각보다 이후이면 충돌로 판단한다.
     *
     * [Output]
     * - 없음
     * - 일정 충돌이 있으면 BAD_REQUEST 예외를 발생시킨다.
     */
    private void validateScheduleConflict(Long planId, PlanUpdateRequest request) {
        boolean existsConflict =
                productionPlanRepository
                        .existsByLineIdAndPlanIdNotAndPlannedStartAtLessThanAndPlannedEndAtGreaterThan(
                                request.getLineId(),
                                planId,
                                request.getPlannedEndAt(),
                                request.getPlannedStartAt()
                        );

        if (existsConflict) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }
}