package s_map.server.domain.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.order.dto.req.OrderCreateRequest;
import s_map.server.domain.order.dto.res.OrderCreateResponse;
import s_map.server.domain.order.dto.res.OrderDetailResponse;
import s_map.server.domain.order.dto.res.OrderListResponse;
import s_map.server.domain.order.entity.CustomerOrder;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.order.repository.CustomerOrderRepository;
import s_map.server.domain.order.repository.LineAssignmentCandidateProjection;
import s_map.server.domain.order.repository.ProductionPlanRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

    private final CustomerOrderRepository customerOrderRepository;
    private final ProductionPlanRepository productionPlanRepository;

    public List<OrderListResponse> getOrders() {
        return customerOrderRepository.findAllOrderSummaries()
                .stream()
                .map(OrderListResponse::from)
                .toList();
    }

    public OrderDetailResponse getOrder(Long orderId) {
        return customerOrderRepository.findOrderDetail(orderId)
                .map(OrderDetailResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        validateOrderCreateRequest(request);

        if (!customerOrderRepository.existsProductById(request.productId())) {
            throw new CustomException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        Long operatorId = resolveOperatorId(request);
        String productName = customerOrderRepository.findProductNameById(request.productId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));

        LineAssignment assignment = assignBestLine(
                request.productId(),
                request.orderQuantity(),
                request.desiredStartAt()
        );

        String orderNo = generateOrderNo();

        CustomerOrder order = CustomerOrder.create(
                orderNo,
                request.productId(),
                request.orderQuantity(),
                request.customerName().trim(),
                request.customerContactName().trim(),
                request.dueDate(),
                request.contractAmount(),
                request.latePenaltyAmount()
        );

        CustomerOrder savedOrder = customerOrderRepository.save(order);

        ProductionPlan plan = ProductionPlan.create(
                savedOrder.getOrderId(),
                savedOrder.getProductId(),
                assignment.lineId(),
                operatorId,
                assignment.plannedStartAt(),
                assignment.plannedEndAt(),
                assignment.estimatedDurationHr(),
                savedOrder.getOrderQuantity(),
                assignment.planSequence()
        );

        ProductionPlan savedPlan = productionPlanRepository.save(plan);

        return OrderCreateResponse.from(
                savedOrder,
                productName,
                savedPlan,
                assignment.lineName()
        );
    }

    private void validateOrderCreateRequest(OrderCreateRequest request) {
        LocalDate today = LocalDate.now();

        if (request.dueDate().isBefore(today)) {
            throw new CustomException(ErrorCode.INVALID_ORDER_DATE);
        }

        if (request.desiredStartAt().toLocalDate().isAfter(request.dueDate())) {
            throw new CustomException(ErrorCode.INVALID_ORDER_DATE);
        }

        boolean hasOperatorId = request.operatorId() != null;
        boolean hasOperatorName = request.operatorName() != null && !request.operatorName().isBlank();

        if (!hasOperatorId && !hasOperatorName) {
            throw new CustomException(ErrorCode.INVALID_ORDER_OPERATOR);
        }
    }

    private Long resolveOperatorId(OrderCreateRequest request) {
        if (request.operatorId() != null) {
            if (!customerOrderRepository.existsActiveOperatorById(request.operatorId())) {
                throw new CustomException(ErrorCode.OPERATOR_NOT_FOUND);
            }
            return request.operatorId();
        }

        return customerOrderRepository.findActiveOperatorIdByName(request.operatorName().trim())
                .orElseThrow(() -> new CustomException(ErrorCode.OPERATOR_NOT_FOUND));
    }

    private LineAssignment assignBestLine(
            Long productId,
            Integer orderQuantity,
            OffsetDateTime desiredStartAt
    ) {
        List<LineAssignmentCandidateProjection> candidates =
                productionPlanRepository.findAssignmentCandidates(productId);

        if (candidates.isEmpty()) {
            throw new CustomException(ErrorCode.AVAILABLE_PRODUCTION_LINE_NOT_FOUND);
        }

        return candidates.stream()
                .map(candidate -> toLineAssignment(candidate, orderQuantity, desiredStartAt))
                .min(
                        Comparator.comparing(LineAssignment::plannedEndAt)
                                .thenComparing(LineAssignment::priorityRank)
                                .thenComparing(LineAssignment::lineId)
                )
                .orElseThrow(() -> new CustomException(ErrorCode.AVAILABLE_PRODUCTION_LINE_NOT_FOUND));
    }

    private LineAssignment toLineAssignment(
            LineAssignmentCandidateProjection candidate,
            Integer orderQuantity,
            OffsetDateTime desiredStartAt
    ) {
        OffsetDateTime plannedStartAt = resolvePlannedStartAt(
                desiredStartAt,
                candidate.getLastPlannedEndAt()
        );

        BigDecimal estimatedDurationHr = calculateEstimatedDurationHr(
                orderQuantity,
                candidate.getCapacityPerDay(),
                candidate.getStandardProductionTimeHr()
        );

        OffsetDateTime plannedEndAt = addHours(plannedStartAt, estimatedDurationHr);

        int lastSequence = candidate.getLastPlanSequence() == null
                ? 0
                : candidate.getLastPlanSequence();

        return new LineAssignment(
                candidate.getLineId(),
                candidate.getLineName(),
                candidate.getPriorityRank(),
                plannedStartAt,
                plannedEndAt,
                estimatedDurationHr,
                lastSequence + 1
        );
    }

    private OffsetDateTime resolvePlannedStartAt(
            OffsetDateTime desiredStartAt,
            OffsetDateTime lastPlannedEndAt
    ) {
        if (lastPlannedEndAt == null) {
            return desiredStartAt;
        }

        if (lastPlannedEndAt.isAfter(desiredStartAt)) {
            return lastPlannedEndAt;
        }

        return desiredStartAt;
    }

    private BigDecimal calculateEstimatedDurationHr(
            Integer orderQuantity,
            Integer capacityPerDay,
            BigDecimal standardProductionTimeHr
    ) {
        if (capacityPerDay != null && capacityPerDay > 0) {
            long requiredDays = ceilDiv(orderQuantity, capacityPerDay);
            return BigDecimal.valueOf(requiredDays)
                    .multiply(BigDecimal.valueOf(24))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        if (standardProductionTimeHr != null && standardProductionTimeHr.compareTo(BigDecimal.ZERO) > 0) {
            return standardProductionTimeHr
                    .multiply(BigDecimal.valueOf(orderQuantity))
                    .setScale(2, RoundingMode.CEILING);
        }

        throw new CustomException(ErrorCode.AVAILABLE_PRODUCTION_LINE_NOT_FOUND);
    }

    private long ceilDiv(int numerator, int denominator) {
        return (numerator + denominator - 1L) / denominator;
    }

    private OffsetDateTime addHours(OffsetDateTime startAt, BigDecimal hours) {
        long minutes = hours
                .multiply(BigDecimal.valueOf(60))
                .setScale(0, RoundingMode.CEILING)
                .longValue();

        return startAt.plus(Duration.ofMinutes(minutes));
    }

    private String generateOrderNo() {
        String prefix = "PO-" + LocalDate.now().format(ORDER_NO_DATE_FORMATTER) + "-";

        int nextSequence = customerOrderRepository.findLatestOrderNoByPrefix(prefix)
                .map(this::extractOrderNoSequence)
                .orElse(0) + 1;

        return prefix + String.format("%03d", nextSequence);
    }

    private int extractOrderNoSequence(String orderNo) {
        int lastDashIndex = orderNo.lastIndexOf('-');

        if (lastDashIndex < 0 || lastDashIndex == orderNo.length() - 1) {
            return 0;
        }

        try {
            return Integer.parseInt(orderNo.substring(lastDashIndex + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private record LineAssignment(
            Long lineId,
            String lineName,
            Integer priorityRank,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            BigDecimal estimatedDurationHr,
            Integer planSequence
    ) {
    }
}