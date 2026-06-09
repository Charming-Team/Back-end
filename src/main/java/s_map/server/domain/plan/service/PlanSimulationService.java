package s_map.server.domain.plan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.order.repository.CustomerOrderRepository;
import s_map.server.domain.order.repository.ProductionPlanRepository;
import s_map.server.domain.plan.dto.req.SelectedPlanSimulationSaveRequest;
import s_map.server.domain.plan.dto.res.PlanSimulationDetailResponse;
import s_map.server.domain.plan.dto.res.PlanSimulationListResponse;
import s_map.server.domain.plan.dto.res.SelectedPlanSimulationSaveResponse;
import s_map.server.domain.plan.repository.PlanSimulationCommandRepository;
import s_map.server.domain.plan.repository.PlanSimulationRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanSimulationService {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");

    private final PlanSimulationRepository planSimulationRepository;
    private final PlanSimulationCommandRepository planSimulationCommandRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final CustomerOrderRepository customerOrderRepository;

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
     * - 요청 plans의 orderId가 customer_orders에 존재하는지 확인한다.
     * - 선택된 plans를 production_plans에 저장한다.
     * - 시뮬레이션 요약을 schedule_simulation_results에 저장한다.
     * - 시뮬레이션 상세 변경 내역을 schedule_simulation_details에 저장한다.
     */
    @Transactional
    public SelectedPlanSimulationSaveResponse saveSelectedSimulation(
            SelectedPlanSimulationSaveRequest request,
            Long appliedBy
    ) {
        validateSaveRequest(request);
        validateOrdersExist(request);

        OffsetDateTime appliedAt = OffsetDateTime.now(DEFAULT_PRODUCTION_ZONE);
        String simulationGroupId = resolveSimulationGroupId(request.getSimulationGroupId(), appliedAt);
        String simulationType = resolveSimulationType(request.getPlanVariantCode());

        Long simulationId = planSimulationCommandRepository.saveSimulationResult(
                request,
                simulationGroupId,
                simulationType,
                appliedBy,
                appliedAt
        );

        List<Long> savedPlanIds = new ArrayList<>();

        for (SelectedPlanSimulationSaveRequest.SelectedPlan selectedPlan : request.getPlans()) {
            ProductionPlan savedPlan = productionPlanRepository.save(
                    ProductionPlan.create(
                            selectedPlan.getOrderId(),
                            selectedPlan.getProductId(),
                            selectedPlan.getLineId(),
                            selectedPlan.getOperatorId(),
                            selectedPlan.getPlannedStartAt(),
                            selectedPlan.getPlannedEndAt(),
                            selectedPlan.getEstimatedDurationHr(),
                            selectedPlan.getPlannedQuantity(),
                            selectedPlan.getPlanSequence()
                    )
            );

            if (selectedPlan.resolvePlanStatus() != savedPlan.getPlanStatus()) {
                savedPlan.updatePlan(
                        savedPlan.getLineId(),
                        savedPlan.getOperatorId(),
                        savedPlan.getPlannedStartAt(),
                        savedPlan.getPlannedEndAt(),
                        savedPlan.getPlannedQuantity(),
                        savedPlan.getPlanSequence(),
                        selectedPlan.resolvePlanStatus()
                );
            }

            savedPlanIds.add(savedPlan.getPlanId());

            planSimulationCommandRepository.saveSimulationDetail(
                    simulationId,
                    savedPlan.getPlanId(),
                    selectedPlan
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
                .appliedAt(appliedAt)
                .build();
    }

    private void validateSaveRequest(SelectedPlanSimulationSaveRequest request) {
        if (request == null || request.getPlans() == null || request.getPlans().isEmpty()) {
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
        }
    }

    private void validateOrdersExist(SelectedPlanSimulationSaveRequest request) {
        for (SelectedPlanSimulationSaveRequest.SelectedPlan plan : request.getPlans()) {
            if (!customerOrderRepository.existsById(plan.getOrderId())) {
                throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
            }
        }
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

    private String resolveSimulationGroupId(String simulationGroupId, OffsetDateTime appliedAt) {
        if (simulationGroupId != null && !simulationGroupId.isBlank()) {
            return simulationGroupId.trim();
        }

        return "SIM-GRP-" + appliedAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
}