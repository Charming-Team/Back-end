package s_map.server.domain.plan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.order.entity.CustomerOrder;
import s_map.server.domain.order.entity.PlanStatus;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.order.repository.CustomerOrderRepository;
import s_map.server.domain.order.repository.ProductionPlanRepository;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateRequest;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateResponse;
import s_map.server.domain.plan.dto.req.PlanAiGenerateRequest;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanAiService {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime DUE_DATE_CUTOFF_TIME = LocalTime.of(8, 59, 59);
    private static final DateTimeFormatter FAST_API_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS Z");

    private final PlanningFastApiClient planningFastApiClient;
    private final ProductionPlanRepository productionPlanRepository;
    private final CustomerOrderRepository customerOrderRepository;

    /**
     * [기능]
     * FastAPI 생산계획 조정 결과를 생성한다.
     *
     * [Input]
     * - request: 충돌난 생산계획 ID, 이동 목표 라인/시간, 재계획 기간
     * - authorizationHeader: Authorization 헤더
     * - refreshToken: refreshToken 쿠키
     *
     * [Process]
     * - planId로 이동 대상 생산계획과 주문 정보를 조회한다.
     * - planningStart~planningEnd 기간에 포함되는 재배치 후보 생산계획을 조회한다.
     * - 이동 대상은 edit_orders.locked_plan으로 고정하고, 후보 계획은 add_orders로 변환한다.
     * - FastAPI 생산계획 조정 API를 호출한다.
     * - 응답 결과는 DB에 저장하지 않고 그대로 반환한다.
     *
     * [Output]
     * - FastApiPlanningGenerateResponse
     */
    public FastApiPlanningGenerateResponse generatePlanning(
            PlanAiGenerateRequest request,
            String authorizationHeader,
            String refreshToken
    ) {
        FastApiPlanningGenerateRequest fastApiRequest = buildFastApiPlanningRequest(request);

        return planningFastApiClient.generatePlanning(
                fastApiRequest,
                authorizationHeader,
                refreshToken
        );
    }

    private FastApiPlanningGenerateRequest buildFastApiPlanningRequest(PlanAiGenerateRequest request) {
        validateRequest(request);

        ProductionPlan targetPlan = productionPlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCTION_PLAN_NOT_FOUND));

        validateEditablePlan(targetPlan);

        Long targetLineId = request.getLineId() == null ? targetPlan.getLineId() : request.getLineId();
        validateLineCapability(targetPlan, targetLineId);

        List<ProductionPlan> addPlans = productionPlanRepository
                .findByPlannedStartAtLessThanAndPlannedEndAtGreaterThanOrderByPlannedStartAtAsc(
                        request.getPlanningEnd(),
                        request.getPlanningStart()
                )
                .stream()
                .filter(this::isReplanningCandidate)
                .filter(plan -> !plan.getPlanId().equals(targetPlan.getPlanId()))
                .toList();

        Map<Long, CustomerOrder> orderMap = findOrderMap(targetPlan, addPlans);
        CustomerOrder targetOrder = findOrder(targetPlan, orderMap);

        FastApiPlanningGenerateRequest.PlanningEditOrder editOrder =
                toEditOrder(targetPlan, targetOrder, targetLineId, request);

        List<FastApiPlanningGenerateRequest.PlanningAddOrder> addOrders = addPlans.stream()
                .map(plan -> toAddOrder(plan, findOrder(plan, orderMap)))
                .toList();

        return FastApiPlanningGenerateRequest.of(
                formatDateTime(request.getPlanningStart()),
                formatDateTime(request.getPlanningEnd()),
                List.of(editOrder),
                addOrders
        );
    }

    private void validateRequest(PlanAiGenerateRequest request) {
        if (request == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST, "AI 생산계획 생성 요청은 필수입니다.");
        }

        if (request.getPlannedStartAt() == null
                || request.getPlannedEndAt() == null
                || request.getPlanningStart() == null
                || request.getPlanningEnd() == null
                || request.getPlanId() == null) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        if (!request.getPlannedStartAt().isBefore(request.getPlannedEndAt())
                || !request.getPlanningStart().isBefore(request.getPlanningEnd())) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        if (request.getPlannedStartAt().isBefore(request.getPlanningStart())
                || request.getPlannedEndAt().isAfter(request.getPlanningEnd())) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private void validateEditablePlan(ProductionPlan plan) {
        if (plan.getPlanStatus() == PlanStatus.COMPLETED
                || plan.getPlanStatus() == PlanStatus.CANCELLED) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }
    }

    private void validateLineCapability(ProductionPlan plan, Long targetLineId) {
        boolean existsActiveLineCapability = productionPlanRepository.existsActiveLineCapability(
                targetLineId,
                plan.getProductId()
        );

        if (!existsActiveLineCapability) {
            throw new CustomException(ErrorCode.AVAILABLE_PRODUCTION_LINE_NOT_FOUND);
        }
    }

    private Map<Long, CustomerOrder> findOrderMap(
            ProductionPlan targetPlan,
            List<ProductionPlan> addPlans
    ) {
        List<Long> orderIds = java.util.stream.Stream.concat(
                        java.util.stream.Stream.of(targetPlan),
                        addPlans.stream()
                )
                .map(ProductionPlan::getOrderId)
                .distinct()
                .toList();

        return customerOrderRepository.findAllById(orderIds)
                .stream()
                .collect(Collectors.toMap(
                        CustomerOrder::getOrderId,
                        Function.identity(),
                        (first, second) -> first
                ));
    }

    private CustomerOrder findOrder(
            ProductionPlan plan,
            Map<Long, CustomerOrder> orderMap
    ) {
        CustomerOrder order = orderMap.get(plan.getOrderId());
        if (order == null) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (!plan.getProductId().equals(order.getProductId())) {
            throw new CustomException(ErrorCode.BAD_REQUEST);
        }

        return order;
    }

    private FastApiPlanningGenerateRequest.PlanningEditOrder toEditOrder(
            ProductionPlan plan,
            CustomerOrder order,
            Long targetLineId,
            PlanAiGenerateRequest request
    ) {
        return FastApiPlanningGenerateRequest.PlanningEditOrder.of(
                plan.getPlanId(),
                plan.getProductId(),
                plan.getPlannedQuantity(),
                formatDueDate(order),
                amountOrZero(order.getContractAmount()),
                amountOrZero(order.getLatePenaltyAmount()),
                plan.getPlanStatus().name(),
                FastApiPlanningGenerateRequest.LockedPlan.of(
                        targetLineId,
                        formatDateTime(request.getPlannedStartAt()),
                        formatDateTime(request.getPlannedEndAt())
                )
        );
    }

    private FastApiPlanningGenerateRequest.PlanningAddOrder toAddOrder(
            ProductionPlan plan,
            CustomerOrder order
    ) {
        return FastApiPlanningGenerateRequest.PlanningAddOrder.of(
                plan.getPlanId(),
                plan.getProductId(),
                plan.getPlannedQuantity(),
                formatDueDate(order),
                amountOrZero(order.getContractAmount()),
                amountOrZero(order.getLatePenaltyAmount()),
                plan.getPlanStatus().name()
        );
    }

    private boolean isReplanningCandidate(ProductionPlan plan) {
        return plan.getPlanStatus() != PlanStatus.COMPLETED
                && plan.getPlanStatus() != PlanStatus.CANCELLED;
    }

    private String formatDateTime(OffsetDateTime value) {
        return value.atZoneSameInstant(DEFAULT_PRODUCTION_ZONE)
                .format(FAST_API_DATE_TIME_FORMATTER);
    }

    private String formatDueDate(CustomerOrder order) {
        return order.getDueDate()
                .atTime(DUE_DATE_CUTOFF_TIME)
                .atZone(DEFAULT_PRODUCTION_ZONE)
                .format(FAST_API_DATE_TIME_FORMATTER);
    }

    private BigDecimal amountOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
