package s_map.server.domain.plan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.order.entity.CustomerOrder;
import s_map.server.domain.order.entity.PlanStatus;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.order.repository.CustomerOrderRepository;
import s_map.server.domain.order.repository.ProductionPlanRepository;
import s_map.server.domain.plan.dto.req.SelectedPlanSimulationSaveRequest;
import s_map.server.domain.plan.dto.res.PlanSimulationDetailResponse;
import s_map.server.domain.plan.dto.res.PlanSimulationListResponse;
import s_map.server.domain.plan.dto.res.SelectedPlanSimulationSaveResponse;
import s_map.server.domain.plan.repository.PlanSimulationCommandRepository;
import s_map.server.domain.plan.repository.PlanSimulationRepository;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.UserStatus;
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanSimulationService {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<String> SCHEDULE_BLOCKING_STATUSES = List.of(
            PlanStatus.SCHEDULED.name(),
            PlanStatus.IN_PROGRESS.name(),
            PlanStatus.DELAYED.name()
    );

    private final PlanSimulationRepository planSimulationRepository;
    private final PlanSimulationCommandRepository planSimulationCommandRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final UserRepository userRepository;

    /**
     * [기능]
     * 생산계획 시뮬레이션 결과 목록을 조회한다.
     */
    public List<PlanSimulationListResponse> getSimulations() {
        return planSimulationRepository.findAllSimulations();
    }

    /**
     * [기능]
     * 특정 생산계획 시뮬레이션의 상세 변경 내역을 조회한다.
     */
    public List<PlanSimulationDetailResponse> getSimulationDetails(Long simulationId) {
        if (!planSimulationRepository.existsSimulationById(simulationId)) {
            throw new CustomException(ErrorCode.PLAN_SIMULATION_NOT_FOUND);
        }

        return planSimulationRepository.findDetailsBySimulationId(simulationId);
    }

    /**
     * [기능]
     * FastAPI 생산계획 조정 결과 중 사용자가 선택한 대안 1개를 DB에 저장한다.
     *
     * [Process]
     * - 요청값을 검증한다.
     * - 요청 plans의 주문, 제품, 생산 라인, 담당자, 일정 충돌 여부를 검증한다.
     * - planId가 있는 요청은 해당 생산계획 row를 수정하고, planId가 없으면 새 계획을 저장한다.
     * - 시뮬레이션 요약을 schedule_simulation_results에 저장한다.
     * - 시뮬레이션 상세 변경 내역을 schedule_simulation_details에 저장한다.
     */
    @Transactional
    public SelectedPlanSimulationSaveResponse saveSelectedSimulation(
            SelectedPlanSimulationSaveRequest request,
            Long appliedBy
    ) {
        validateBaseRequest(request);

        OffsetDateTime selectedAt = OffsetDateTime.now(DEFAULT_PRODUCTION_ZONE);
        String simulationGroupId = resolveSimulationGroupId(request.getSimulationGroupId(), selectedAt);
        String simulationType = resolveSimulationType(request.getPlanVariantCode());
        boolean baselineSelection = isCurrentPlanBaseline(simulationType);

        Long simulationId = planSimulationCommandRepository.saveSimulationResult(
                request,
                simulationGroupId,
                simulationType,
                baselineSelection ? null : appliedBy,
                baselineSelection ? null : selectedAt,
                selectedAt
        );

        if (baselineSelection) {
            return SelectedPlanSimulationSaveResponse.builder()
                    .simulationId(simulationId)
                    .simulationGroupId(simulationGroupId)
                    .savedPlanCount(0)
                    .savedDetailCount(0)
                    .savedPlanIds(List.of())
                    .applied(false)
                    .appliedBy(null)
                    .appliedAt(null)
                    .build();
        }

        validateSaveRequest(request);
        Map<Long, CustomerOrder> ordersById = loadOrdersById(request);
        Map<Long, ProductionPlan> plansByPlanId = loadEditablePlansByPlanId(request);
        validateSelectedPlans(request, ordersById, plansByPlanId);

        List<Long> savedPlanIds = new ArrayList<>();
        releasePlanSequencesTemporarily(plansByPlanId.values());

        for (SelectedPlanSimulationSaveRequest.SelectedPlan selectedPlan : request.getPlans()) {
            ProductionPlan savedPlan = applySelectedPlan(
                    selectedPlan,
                    resolveExistingPlan(selectedPlan, plansByPlanId)
            );

            savedPlanIds.add(savedPlan.getPlanId());

            planSimulationCommandRepository.saveSimulationDetail(
                    simulationId,
                    savedPlan.getPlanId(),
                    selectedPlan,
                    selectedAt
            );
        }

        return SelectedPlanSimulationSaveResponse.builder()
                .simulationId(simulationId)
                .simulationGroupId(simulationGroupId)
                .savedPlanCount(savedPlanIds.size())
                .savedDetailCount(savedPlanIds.size())
                .savedPlanIds(savedPlanIds)
                .applied(true)
                .appliedBy(appliedBy)
                .appliedAt(selectedAt)
                .build();
    }

    private void validateBaseRequest(SelectedPlanSimulationSaveRequest request) {
        if (request == null || request.getPlans() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private void validateSaveRequest(SelectedPlanSimulationSaveRequest request) {
        if (request.getPlans().isEmpty()) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        for (SelectedPlanSimulationSaveRequest.SelectedPlan plan : request.getPlans()) {
            if (plan.getPlannedStartAt() == null
                    || plan.getPlannedEndAt() == null
                    || !plan.getPlannedStartAt().isBefore(plan.getPlannedEndAt())) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }

            if (plan.getEstimatedDurationHr() == null
                    || plan.getEstimatedDurationHr().signum() <= 0) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }

            if (plan.getPlannedQuantity() == null || plan.getPlannedQuantity() <= 0) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }

            if (plan.getPlanSequence() == null || plan.getPlanSequence() <= 0) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }

            if (plan.getAfterDelayed() == null) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }

            Long planId = plan.resolvePlanId();
            if (planId != null && planId <= 0) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }
        }
    }

    private Map<Long, CustomerOrder> loadOrdersById(SelectedPlanSimulationSaveRequest request) {
        List<Long> orderIds = request.getPlans()
                .stream()
                .map(SelectedPlanSimulationSaveRequest.SelectedPlan::getOrderId)
                .distinct()
                .toList();

        Map<Long, CustomerOrder> ordersById = customerOrderRepository.findAllById(orderIds)
                .stream()
                .collect(Collectors.toMap(CustomerOrder::getOrderId, Function.identity()));

        if (ordersById.size() != orderIds.size()) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }

        return ordersById;
    }

    private Map<Long, ProductionPlan> loadEditablePlansByPlanId(SelectedPlanSimulationSaveRequest request) {
        List<Long> planIds = request.getPlans()
                .stream()
                .map(SelectedPlanSimulationSaveRequest.SelectedPlan::resolvePlanId)
                .filter(java.util.Objects::nonNull)
                .toList();

        if (planIds.isEmpty()) {
            return Map.of();
        }

        if (new HashSet<>(planIds).size() != planIds.size()) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        Map<Long, ProductionPlan> plansByPlanId = productionPlanRepository.findAllById(planIds)
                .stream()
                .collect(Collectors.toMap(ProductionPlan::getPlanId, Function.identity()));

        if (plansByPlanId.size() != planIds.size()) {
            throw new CustomException(ErrorCode.PRODUCTION_PLAN_NOT_FOUND);
        }

        for (ProductionPlan plan : plansByPlanId.values()) {
            if (plan.getPlanStatus() == PlanStatus.COMPLETED
                    || plan.getPlanStatus() == PlanStatus.CANCELLED) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }
        }

        return plansByPlanId;
    }

    private void validateSelectedPlans(
            SelectedPlanSimulationSaveRequest request,
            Map<Long, CustomerOrder> ordersById,
            Map<Long, ProductionPlan> plansByPlanId
    ) {
        List<Long> excludedPlanIds = excludedPlanIds(plansByPlanId.values());
        validateDuplicateLineSequences(request.getPlans());
        validateScheduleOverlapsInRequest(request.getPlans());

        for (SelectedPlanSimulationSaveRequest.SelectedPlan plan : request.getPlans()) {
            CustomerOrder order = ordersById.get(plan.getOrderId());
            ProductionPlan existingPlan = resolveExistingPlan(plan, plansByPlanId);

            if (!order.getProductId().equals(plan.getProductId())) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }

            if (existingPlan != null
                    && (!existingPlan.getOrderId().equals(plan.getOrderId())
                    || !existingPlan.getProductId().equals(plan.getProductId()))) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }

            validateLineCapability(plan);
            validateOperator(plan);
            validateLineSequence(plan, excludedPlanIds);
            validateScheduleConflict(plan, excludedPlanIds);
        }
    }

    private List<Long> excludedPlanIds(Collection<ProductionPlan> plans) {
        List<Long> planIds = plans.stream()
                .map(ProductionPlan::getPlanId)
                .toList();

        return planIds.isEmpty() ? List.of(Long.MIN_VALUE) : planIds;
    }

    private ProductionPlan resolveExistingPlan(
            SelectedPlanSimulationSaveRequest.SelectedPlan plan,
            Map<Long, ProductionPlan> plansByPlanId
    ) {
        Long planId = plan.resolvePlanId();
        return planId == null ? null : plansByPlanId.get(planId);
    }

    private void validateDuplicateLineSequences(List<SelectedPlanSimulationSaveRequest.SelectedPlan> plans) {
        Set<LineSequenceKey> keys = new HashSet<>();

        for (SelectedPlanSimulationSaveRequest.SelectedPlan plan : plans) {
            if (!keys.add(new LineSequenceKey(plan.getLineId(), plan.getPlanSequence()))) {
                throw new CustomException(ErrorCode.BAD_REQUEST);
            }
        }
    }

    private void validateScheduleOverlapsInRequest(List<SelectedPlanSimulationSaveRequest.SelectedPlan> plans) {
        for (int left = 0; left < plans.size(); left++) {
            SelectedPlanSimulationSaveRequest.SelectedPlan leftPlan = plans.get(left);

            for (int right = left + 1; right < plans.size(); right++) {
                SelectedPlanSimulationSaveRequest.SelectedPlan rightPlan = plans.get(right);

                if (leftPlan.getLineId().equals(rightPlan.getLineId())
                        && leftPlan.getPlannedStartAt().isBefore(rightPlan.getPlannedEndAt())
                        && leftPlan.getPlannedEndAt().isAfter(rightPlan.getPlannedStartAt())) {
                    throw new CustomException(ErrorCode.PLAN_SCHEDULE_CONFLICT);
                }
            }
        }
    }

    private void validateLineCapability(SelectedPlanSimulationSaveRequest.SelectedPlan plan) {
        boolean existsActiveLineCapability = productionPlanRepository.existsActiveLineCapability(
                plan.getLineId(),
                plan.getProductId()
        );

        if (!existsActiveLineCapability) {
            throw new CustomException(ErrorCode.AVAILABLE_PRODUCTION_LINE_NOT_FOUND);
        }
    }

    private void validateOperator(SelectedPlanSimulationSaveRequest.SelectedPlan plan) {
        if (plan.getOperatorId() == null) {
            return;
        }

        boolean existsActiveOperator = userRepository.existsByIdAndStatusAndRole(
                plan.getOperatorId(),
                UserStatus.ACTIVE,
                Role.OPERATOR
        );

        if (!existsActiveOperator) {
            throw new CustomException(ErrorCode.OPERATOR_NOT_FOUND);
        }
    }

    private void validateLineSequence(
            SelectedPlanSimulationSaveRequest.SelectedPlan plan,
            List<Long> excludedPlanIds
    ) {
        boolean existsLineSequence = productionPlanRepository.existsLineSequenceOutsidePlans(
                plan.getLineId(),
                plan.getPlanSequence(),
                excludedPlanIds
        );

        if (existsLineSequence) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private void validateScheduleConflict(
            SelectedPlanSimulationSaveRequest.SelectedPlan plan,
            List<Long> excludedPlanIds
    ) {
        boolean existsScheduleConflict = productionPlanRepository.existsScheduleConflictOutsidePlans(
                plan.getLineId(),
                plan.getPlannedStartAt(),
                plan.getPlannedEndAt(),
                SCHEDULE_BLOCKING_STATUSES,
                excludedPlanIds
        );

        if (existsScheduleConflict) {
            throw new CustomException(ErrorCode.PLAN_SCHEDULE_CONFLICT);
        }
    }

    private void releasePlanSequencesTemporarily(Collection<ProductionPlan> plans) {
        if (plans.isEmpty()) {
            return;
        }

        int temporarySequence = productionPlanRepository.findMaxPlanSequence() + 1_000;

        for (ProductionPlan plan : plans) {
            plan.updatePlan(
                    plan.getLineId(),
                    plan.getOperatorId(),
                    plan.getPlannedStartAt(),
                    plan.getPlannedEndAt(),
                    plan.getPlannedQuantity(),
                    temporarySequence++,
                    plan.getPlanStatus()
            );
        }

        productionPlanRepository.flush();
    }

    private ProductionPlan applySelectedPlan(
            SelectedPlanSimulationSaveRequest.SelectedPlan selectedPlan,
            ProductionPlan existingPlan
    ) {
        if (existingPlan == null) {
            ProductionPlan newPlan = ProductionPlan.create(
                    selectedPlan.getOrderId(),
                    selectedPlan.getProductId(),
                    selectedPlan.getLineId(),
                    selectedPlan.getOperatorId(),
                    selectedPlan.getPlannedStartAt(),
                    selectedPlan.getPlannedEndAt(),
                    selectedPlan.getEstimatedDurationHr(),
                    selectedPlan.getPlannedQuantity(),
                    selectedPlan.getPlanSequence()
            );
            return productionPlanRepository.save(newPlan);
        }

        existingPlan.applySelectedSimulationPlan(
                selectedPlan.getLineId(),
                selectedPlan.getOperatorId(),
                selectedPlan.getPlannedStartAt(),
                selectedPlan.getPlannedEndAt(),
                selectedPlan.getEstimatedDurationHr(),
                selectedPlan.getPlannedQuantity(),
                selectedPlan.getPlanSequence(),
                selectedPlan.resolvePlanStatus()
        );

        return existingPlan;
    }

    private String resolveSimulationType(String planVariantCode) {
        if (planVariantCode == null || planVariantCode.isBlank()) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        String normalized = planVariantCode.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "DUE_DATE_OPTIMAL" -> "DUE_DATE_OPTIMIZATION";
            case "AMOUNT_OPTIMAL" -> "COST_OPTIMIZATION";
            case "CURRENT_PLAN_BASELINE" -> "CURRENT_PLAN_BASELINE";
            default -> throw new CustomException(ErrorCode.BAD_REQUEST);
        };
    }

    private boolean isCurrentPlanBaseline(String simulationType) {
        return "CURRENT_PLAN_BASELINE".equals(simulationType);
    }

    private String resolveSimulationGroupId(String simulationGroupId, OffsetDateTime appliedAt) {
        if (simulationGroupId != null && !simulationGroupId.isBlank()) {
            return simulationGroupId.trim();
        }

        return "SIM-GRP-" + appliedAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private record LineSequenceKey(Long lineId, Integer planSequence) {
    }
}
