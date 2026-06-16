package s_map.server.domain.plan.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.material.entity.ProductionPlanMaterial;
import s_map.server.domain.material.repository.ProductionPlanMaterialRepository;
import s_map.server.domain.plan.dto.req.PlanScheduleUpdateRequest;
import s_map.server.domain.plan.dto.req.PlanUpdateRequest;
import s_map.server.domain.plan.dto.res.CurrentPlanResponse;
import s_map.server.domain.plan.dto.res.PlanDetailResponse;
import s_map.server.domain.plan.dto.res.PlanListResponse;
import s_map.server.domain.plan.dto.res.PlanUpdateResponse;
import s_map.server.domain.order.entity.PlanStatus;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.order.repository.ProductionPlanRepository;
import s_map.server.domain.notification.service.NotificationService;
import s_map.server.domain.plan.repository.PlanQueryRepository;
import s_map.server.domain.plan.repository.PlanRow;
import s_map.server.domain.plan.repository.ProductionResultRepository;
import s_map.server.domain.plan.repository.ProductionResultRow;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.UserStatus;
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import s_map.server.domain.risk.service.RiskPredictionEventPublisher;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PlanService {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");

    private final ProductionPlanRepository productionPlanRepository;
    private final PlanQueryRepository planQueryRepository;
    private final ProductionResultRepository productionResultRepository;
    private final UserRepository userRepository;
    private final RiskPredictionEventPublisher riskPredictionEventPublisher;
    private final NotificationService notificationService;
    /*
     * ProductionPlanMaterial은 현재 material 도메인에 위치함.
     * 생산계획 상세 조회 시 해당 계획에 필요한 자재 목록을 조회하기 위해 참조한다.
     */
    private final ProductionPlanMaterialRepository productionPlanMaterialRepository;

    /**
     * [기능]
     * 조건 없이 전체 생산계획 목록을 조회한다.
     *
     * [Input]
     * - 없음
     *
     * [Process]
     * - 조건 조회 메서드에 검색어, 상태, 기간 조건을 null로 전달한다.
     * - 전체 생산계획을 기본 정렬 기준으로 조회한다.
     *
     * [Output]
     * - List<PlanListResponse>
     * - 전체 생산계획 목록을 반환한다.
     */
    public List<PlanListResponse> getPlans() {
        return getPlans(null, null, null, null);
    }

    /**
     * [기능]
     * 검색어, 상태, 기간 조건으로 생산계획 목록을 조회한다.
     *
     * [Input]
     * - keyword: 계획 ID, 주문 ID, 제품명, 라인명, 라인 코드, 담당자명 검색어
     * - status: 생산계획 상태 코드
     * - startAt: 조회 시작 일시
     * - endAt: 조회 종료 일시
     *
     * [Process]
     * - 검색어는 공백 제거 후 빈 문자열이면 null로 처리한다.
     * - 상태 값은 PlanStatus enum 값인지 검증하고 대문자 코드로 정규화한다.
     * - PlanQueryRepository에서 production_plans, products, production_lines, users를 조인 조회한다.
     * - 기간 조건은 plannedEndAt >= startAt, plannedStartAt < endAt 기준으로 겹치는 일정을 조회한다.
     * - 조회 결과를 프론트엔드 응답용 DTO 목록으로 변환한다.
     *
     * [Output]
     * - List<PlanListResponse>
     * - 생산계획 ID, 주문 ID, 제품 정보, 라인 정보, 담당자 정보,
     *   계획 시작/종료 시간, 예상 소요 시간, 계획 수량, 계획 순서, 상태, 생성/수정 일시를 반환한다.
     */
    public List<PlanListResponse> getPlans(
            String keyword,
            String status,
            OffsetDateTime startAt,
            OffsetDateTime endAt
    ) {
        return planQueryRepository.findPlans(
                        normalizeKeyword(keyword),
                        normalizeStatus(status),
                        startAt,
                        endAt
                )
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
     * - planId로 production_plans, products, production_lines, users를 조인 조회한다.
     * - 생산계획이 존재하지 않으면 PRODUCTION_PLAN_NOT_FOUND 예외를 발생시킨다.
     * - production_plan_materials 테이블에서 해당 생산계획에 필요한 자재 목록을 조회한다.
     * - 생산계획 기본 정보, 표시용 이름, 자재 소요/예약/부족 정보를 하나의 DTO로 조합한다.
     *
     * [Output]
     * - PlanDetailResponse
     * - 생산계획 상세 정보와 계획별 필요 자재 목록을 반환한다.
     */
    public PlanDetailResponse getPlan(Long planId) {
        PlanRow plan = planQueryRepository.findPlanById(planId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCTION_PLAN_NOT_FOUND));

        List<ProductionPlanMaterial> planMaterials =
                productionPlanMaterialRepository.findByPlanIdWithMaterial(planId);

        return PlanDetailResponse.of(plan, planMaterials);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        try {
            return PlanStatus.valueOf(normalizedStatus).name();
        } catch (IllegalArgumentException ignored) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
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
        LocalDate today = LocalDate.now(DEFAULT_PRODUCTION_ZONE);
        OffsetDateTime startOfDay = today.atStartOfDay(DEFAULT_PRODUCTION_ZONE).toOffsetDateTime();
        OffsetDateTime endExclusive = today.plusDays(1).atStartOfDay(DEFAULT_PRODUCTION_ZONE).toOffsetDateTime();

        List<ProductionPlan> plans =
                productionPlanRepository
                        .findByPlannedStartAtLessThanAndPlannedEndAtGreaterThanOrderByPlannedStartAtAsc(
                                endExclusive,
                                startOfDay
                        )
                        .stream()
                        .filter(this::isCurrentTarget)
                        .toList();

        List<Long> planIds = plans.stream()
                .map(ProductionPlan::getPlanId)
                .toList();

        Map<Long, ProductionResultRow> resultMap = planIds.isEmpty()
                ? Map.of()
                : productionResultRepository.findByPlanIdIn(planIds)
                        .stream()
                        .collect(Collectors.toMap(
                                ProductionResultRow::planId,
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

    private boolean isCurrentTarget(ProductionPlan plan) {
        return plan.getPlanStatus() == PlanStatus.SCHEDULED
                || plan.getPlanStatus() == PlanStatus.IN_PROGRESS
                || plan.getPlanStatus() == PlanStatus.DELAYED;
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
     * - 변경 라인에서 해당 제품을 생산할 수 있는지 검증한다.
     * - 담당자가 지정된 경우 활성 작업자인지 검증한다.
     * - 동일 라인의 라인 내 순서 중복 여부를 검증한다.
     * - 동일 라인에 겹치는 일정이 있는지 검증한다.
     * - production_plans 테이블에 수정 내용을 반영한다.
     *
     * [Output]
     * - PlanUpdateResponse
     * - 실제 반영 여부가 true인 수정 결과를 반환한다.
     */
    @Transactional
    public PlanUpdateResponse updatePlan(Long planId, PlanUpdateRequest request) {
        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCTION_PLAN_NOT_FOUND));

        validateEditablePlan(plan);
        validateUpdateRequest(request);
        validateLineCapability(plan, request.getLineId());
        validateOperator(request);
        validateLineSequence(planId, request);
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
        riskPredictionEventPublisher.publishPlanChanged(plan.getOrderId());
        notifyScheduleAppliedSafely(plan);

        return PlanUpdateResponse.from(plan);
    }

    /**
     * [기능]
     * 캘린더 드래그 앤 드롭으로 생산계획 일정을 이동한다.
     *
     * [Input]
     * - planId: 이동할 생산계획 ID
     * - request: 이동 후 라인 ID와 계획 시작/종료 일시
     *
     * [Process]
     * - 생산계획이 없으면 PRODUCTION_PLAN_NOT_FOUND 예외를 발생시킨다.
     * - 완료 또는 취소 상태의 생산계획이면 이동할 수 없도록 차단한다.
     * - 요청 기간이 유효한지 검증한다.
     * - lineId가 없으면 기존 라인을 유지한다.
     * - 변경 라인에서 해당 제품을 생산할 수 있는지 검증한다.
     * - 같은 라인에 일정이 겹치면 PLAN_SCHEDULE_CONFLICT 예외를 발생시킨다.
     * - 일정이 비어 있으면 production_plans의 라인과 계획 시작/종료 일시만 수정한다.
     *
     * [Output]
     * - PlanUpdateResponse
     * - 실제 반영 여부가 true인 일정 이동 결과를 반환한다.
     */
    @Transactional
    public PlanUpdateResponse movePlanSchedule(Long planId, PlanScheduleUpdateRequest request) {
        ProductionPlan plan = productionPlanRepository.findById(planId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCTION_PLAN_NOT_FOUND));

        validateEditablePlan(plan);
        validateScheduleMoveRequest(request);

        Long targetLineId = request.getLineId() == null ? plan.getLineId() : request.getLineId();
        validateLineCapability(plan, targetLineId);

        if (!targetLineId.equals(plan.getLineId())) {
            validateLineSequence(planId, targetLineId, plan.getPlanSequence());
        }

        validateScheduleConflict(
                planId,
                targetLineId,
                request.getPlannedStartAt(),
                request.getPlannedEndAt()
        );

        plan.moveSchedule(
                targetLineId,
                request.getPlannedStartAt(),
                request.getPlannedEndAt()
        );
        riskPredictionEventPublisher.publishPlanChanged(plan.getOrderId());
        notifyScheduleAppliedSafely(plan);

        return PlanUpdateResponse.from(plan);
    }

    private void notifyScheduleAppliedSafely(ProductionPlan plan) {
        try {
            notificationService.createScheduleAppliedNotification(plan);
        } catch (Exception exception) {
            // 알림 저장 실패가 생산계획 변경 트랜잭션을 막지 않도록 로그만 남긴다.
            Long planId = plan == null ? null : plan.getPlanId();
            log.warn("[PlanService] 생산계획 변경 알림 저장 실패 planId={}", planId, exception);
        }
    }

    private void validateLineCapability(ProductionPlan plan, Long lineId) {
        boolean existsActiveLineCapability = productionPlanRepository.existsActiveLineCapability(
                lineId,
                plan.getProductId()
        );

        if (!existsActiveLineCapability) {
            throw new CustomException(ErrorCode.AVAILABLE_PRODUCTION_LINE_NOT_FOUND);
        }
    }

    private void validateOperator(PlanUpdateRequest request) {
        if (request.getOperatorId() == null) {
            return;
        }

        boolean existsActiveOperator = userRepository.existsByIdAndStatusAndRole(
                request.getOperatorId(),
                UserStatus.ACTIVE,
                Role.OPERATOR
        );

        if (!existsActiveOperator) {
            throw new CustomException(ErrorCode.OPERATOR_NOT_FOUND);
        }
    }

    private void validateLineSequence(Long planId, PlanUpdateRequest request) {
        validateLineSequence(planId, request.getLineId(), request.getPlanSequence());
    }

    private void validateLineSequence(Long planId, Long lineId, Integer planSequence) {
        boolean existsSameLineSequence =
                productionPlanRepository.existsByLineIdAndPlanIdNotAndPlanSequence(
                        lineId,
                        planId,
                        planSequence
                );

        if (existsSameLineSequence) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private void validateScheduleMoveRequest(PlanScheduleUpdateRequest request) {
        if (request.getPlannedStartAt() == null
                || request.getPlannedEndAt() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        if (!request.getPlannedStartAt().isBefore(request.getPlannedEndAt())) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
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
        validateScheduleConflict(
                planId,
                request.getLineId(),
                request.getPlannedStartAt(),
                request.getPlannedEndAt()
        );
    }

    private void validateScheduleConflict(
            Long planId,
            Long lineId,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt
    ) {
        boolean existsConflict =
                productionPlanRepository
                        .existsByLineIdAndPlanIdNotAndPlannedStartAtLessThanAndPlannedEndAtGreaterThan(
                                lineId,
                                planId,
                                plannedEndAt,
                                plannedStartAt
                        );

        if (existsConflict) {
            throw new CustomException(ErrorCode.PLAN_SCHEDULE_CONFLICT);
        }
    }
}
