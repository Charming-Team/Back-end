package s_map.server.domain.order.service;

import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.order.dto.req.OrderCreateRequest;
import s_map.server.domain.order.dto.res.OrderCreateResponse;
import s_map.server.domain.order.dto.res.OrderDetailResponse;
import s_map.server.domain.order.dto.res.OrderListResponse;
import s_map.server.domain.order.dto.res.OrderNoPreviewResponse;
import s_map.server.domain.order.entity.CustomerOrder;
import s_map.server.domain.order.entity.OrderStatus;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.order.repository.CustomerOrderRepository;
import s_map.server.domain.order.repository.LineAssignmentCandidateProjection;
import s_map.server.domain.order.repository.OrderNoSequenceRepository;
import s_map.server.domain.order.repository.OrderQueryRepository;
import s_map.server.domain.order.repository.ProductQueryRepository;
import s_map.server.domain.order.repository.ProductionPlanRepository;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.entity.UserStatus;
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate MIN_DUE_DATE_FILTER = LocalDate.of(1, 1, 1);
    private static final LocalDate MAX_DUE_DATE_FILTER = LocalDate.of(9999, 12, 31);
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;
    private static final Set<String> CONCURRENT_ORDER_CONSTRAINT_NAMES = Set.of(
            "uk_customer_orders_order_no",
            "customer_orders_order_no_key",
            "uk_production_plans_line_sequence"
    );

    private final CustomerOrderRepository customerOrderRepository;
    private final OrderQueryRepository orderQueryRepository;
    private final ProductQueryRepository productQueryRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final OrderNoSequenceRepository orderNoSequenceRepository;
    private final UserRepository userRepository;

    public Page<OrderListResponse> getOrders(
            int page,
            int size,
            String keyword,
            OrderStatus status,
            String customerName,
            Long productId,
            LocalDate dueDateFrom,
            LocalDate dueDateTo
    ) {
        validateDueDateRange(dueDateFrom, dueDateTo);

        Pageable pageable = createPageable(page, size);
        String normalizedKeyword = normalize(keyword);
        String normalizedCustomerName = normalize(customerName);
        LocalDate effectiveDueDateFrom = dueDateFrom == null ? MIN_DUE_DATE_FILTER : dueDateFrom;
        LocalDate effectiveDueDateTo = dueDateTo == null ? MAX_DUE_DATE_FILTER : dueDateTo;
        LocalDate today = LocalDate.now(DEFAULT_PRODUCTION_ZONE);
        OffsetDateTime now = OffsetDateTime.now(DEFAULT_PRODUCTION_ZONE);

        if (status == null) {
            List<OrderListResponse> content = orderQueryRepository.findOrderSummariesWithoutStatusFilter(
                            normalizedKeyword,
                            normalizedCustomerName,
                            productId,
                            effectiveDueDateFrom,
                            effectiveDueDateTo,
                            pageable.getPageSize(),
                            pageable.getOffset(),
                            today,
                            now
                    )
                    .stream()
                    .map(OrderListResponse::from)
                    .toList();

            long total = orderQueryRepository.countOrderSummariesWithoutStatusFilter(
                    normalizedKeyword,
                    normalizedCustomerName,
                    productId,
                    effectiveDueDateFrom,
                    effectiveDueDateTo
            );

            return new PageImpl<>(content, pageable, total);
        }

        return orderQueryRepository.findOrderSummaries(
                        normalizedKeyword,
                        status.name(),
                        normalizedCustomerName,
                        productId,
                        effectiveDueDateFrom,
                        effectiveDueDateTo,
                        today,
                        now,
                        pageable
                )
                .map(OrderListResponse::from);
    }

    public OrderDetailResponse getOrder(Long orderId) {
        LocalDate today = LocalDate.now(DEFAULT_PRODUCTION_ZONE);
        OffsetDateTime now = OffsetDateTime.now(DEFAULT_PRODUCTION_ZONE);

        return orderQueryRepository.findOrderDetail(orderId, today, now)
                .map(OrderDetailResponse::from)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    public OrderNoPreviewResponse getNextOrderNoPreview() {
        LocalDate today = LocalDate.now(DEFAULT_PRODUCTION_ZONE);
        int latestPersistedSequence = findLatestPersistedOrderNoSequence(today);
        int latestIssuedSequence = orderNoSequenceRepository.findLastSequence(today)
                .orElse(0);
        int previewSequence = Math.max(latestPersistedSequence, latestIssuedSequence) + 1;

        return OrderNoPreviewResponse.preview(formatOrderNo(today, previewSequence));
    }

    @Transactional
    public OrderCreateResponse createOrder(OrderCreateRequest request) {
        try {
            return createOrderInternal(request);
        } catch (DataIntegrityViolationException exception) {
            if (isConcurrentOrderCreation(exception)) {
                throw new CustomException(ErrorCode.CONCURRENT_ORDER_CREATION);
            }

            throw exception;
        }
    }

    private OrderCreateResponse createOrderInternal(OrderCreateRequest request) {
        OffsetDateTime desiredStartAt = resolveDesiredStartAt(request);
        validateOrderCreateRequest(request, desiredStartAt);

        String productName = productQueryRepository.findProductNameById(request.productId())
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        Long operatorId = resolveOperatorId(request);

        LineAssignment assignment = assignBestLine(
                request.productId(),
                request.orderQuantity(),
                desiredStartAt
        );

        validatePlannedEndAt(assignment.plannedEndAt(), request.dueDate());

        String orderNo = generateOrderNo();

        CustomerOrder order = CustomerOrder.create(
                orderNo,
                request.productId(),
                request.orderQuantity(),
                request.customerName().trim(),
                request.customerContactName().trim(),
                LocalDate.now(DEFAULT_PRODUCTION_ZONE),
                request.dueDate(),
                request.contractAmount(),
                request.latePenaltyAmount()
        );

        CustomerOrder savedOrder = customerOrderRepository.saveAndFlush(order);

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

        ProductionPlan savedPlan = productionPlanRepository.saveAndFlush(plan);

        return OrderCreateResponse.from(
                savedOrder,
                productName,
                savedPlan,
                assignment.lineName()
        );
    }

    private void validateOrderCreateRequest(OrderCreateRequest request, OffsetDateTime desiredStartAt) {
        LocalDate today = LocalDate.now(DEFAULT_PRODUCTION_ZONE);

        if (request.dueDate().isBefore(today)) {
            throw new CustomException(ErrorCode.INVALID_ORDER_DATE);
        }

        if (request.desiredStartAt() != null
                && desiredStartAt.isBefore(OffsetDateTime.now(DEFAULT_PRODUCTION_ZONE))) {
            throw new CustomException(ErrorCode.INVALID_ORDER_DATE);
        }

        if (desiredStartAt.toLocalDate().isBefore(today)
                || desiredStartAt.toLocalDate().isAfter(request.dueDate())) {
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
            if (!userRepository.existsByIdAndStatusAndRole(request.operatorId(), UserStatus.ACTIVE, Role.OPERATOR)) {
                throw new CustomException(ErrorCode.OPERATOR_NOT_FOUND);
            }
            return request.operatorId();
        }

        return userRepository.findFirstByNameAndStatusAndRoleOrderByIdAsc(
                        request.operatorName().trim(),
                        UserStatus.ACTIVE,
                        Role.OPERATOR
                )
                .map(User::getId)
                .orElseThrow(() -> new CustomException(ErrorCode.OPERATOR_NOT_FOUND));
    }

    private LineAssignment assignBestLine(
            Long productId,
            Integer orderQuantity,
            OffsetDateTime desiredStartAt
    ) {
        List<Long> lockedLineIds = productionPlanRepository.lockAssignableLineIds(productId);
        if (lockedLineIds.isEmpty()) {
            throw new CustomException(ErrorCode.AVAILABLE_PRODUCTION_LINE_NOT_FOUND);
        }

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
                toOffsetDateTime(candidate.getLastPlannedEndAt())
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

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }

        return instant.atZone(DEFAULT_PRODUCTION_ZONE).toOffsetDateTime();
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

    private OffsetDateTime resolveDesiredStartAt(OrderCreateRequest request) {
        if (request.desiredStartAt() != null) {
            return request.desiredStartAt();
        }

        if (request.productionStartDate() != null) {
            return request.productionStartDate()
                    .atStartOfDay(DEFAULT_PRODUCTION_ZONE)
                    .toOffsetDateTime();
        }

        throw new CustomException(ErrorCode.INVALID_ORDER_DATE);
    }

    private void validatePlannedEndAt(OffsetDateTime plannedEndAt, LocalDate dueDate) {
        if (plannedEndAt.toLocalDate().isAfter(dueDate)) {
            throw new CustomException(ErrorCode.ORDER_SCHEDULE_EXCEEDS_DUE_DATE);
        }
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
        LocalDate today = LocalDate.now(DEFAULT_PRODUCTION_ZONE);
        int initialSequence = findLatestPersistedOrderNoSequence(today) + 1;
        int nextSequence = orderNoSequenceRepository.nextSequence(today, initialSequence);

        return formatOrderNo(today, nextSequence);
    }

    private int findLatestPersistedOrderNoSequence(LocalDate date) {
        String prefix = createOrderNoPrefix(date);

        return customerOrderRepository.findLatestOrderNoByPrefix(prefix)
                .map(this::extractOrderNoSequence)
                .orElse(0);
    }

    private String createOrderNoPrefix(LocalDate date) {
        return "PO-" + date.format(ORDER_NO_DATE_FORMATTER) + "-";
    }

    private String formatOrderNo(LocalDate date, int sequence) {
        return createOrderNoPrefix(date) + String.format("%03d", sequence);
    }

    private int extractOrderNoSequence(String orderNo) {
        int sequenceIndex = orderNo.lastIndexOf('-') + 1;

        if (sequenceIndex <= 0 || sequenceIndex >= orderNo.length()) {
            return 0;
        }

        try {
            return Integer.parseInt(orderNo.substring(sequenceIndex));
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private boolean isConcurrentOrderCreation(DataIntegrityViolationException exception) {
        return findConstraintName(exception)
                .map(constraintName -> constraintName.toLowerCase(Locale.ROOT))
                .filter(CONCURRENT_ORDER_CONSTRAINT_NAMES::contains)
                .isPresent();
    }

    private Optional<String> findConstraintName(Throwable throwable) {
        Throwable current = throwable;

        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolationException
                    && constraintViolationException.getConstraintName() != null) {
                return Optional.of(constraintViolationException.getConstraintName());
            }

            Optional<String> postgresConstraintName = findPostgresConstraintName(current);
            if (postgresConstraintName.isPresent()) {
                return postgresConstraintName;
            }

            current = current.getCause();
        }

        return Optional.empty();
    }

    private Optional<String> findPostgresConstraintName(Throwable throwable) {
        if (!"org.postgresql.util.PSQLException".equals(throwable.getClass().getName())) {
            return Optional.empty();
        }

        try {
            Method getServerErrorMessage = throwable.getClass().getMethod("getServerErrorMessage");
            Object serverErrorMessage = getServerErrorMessage.invoke(throwable);
            if (serverErrorMessage == null) {
                return Optional.empty();
            }

            Method getConstraint = serverErrorMessage.getClass().getMethod("getConstraint");
            Object constraint = getConstraint.invoke(serverErrorMessage);
            if (constraint instanceof String constraintName && !constraintName.isBlank()) {
                return Optional.of(constraintName);
            }
        } catch (ReflectiveOperationException exception) {
            return Optional.empty();
        }

        return Optional.empty();
    }

    private void validateDueDateRange(LocalDate dueDateFrom, LocalDate dueDateTo) {
        if (dueDateFrom != null && dueDateTo != null && dueDateFrom.isAfter(dueDateTo)) {
            throw new CustomException(ErrorCode.INVALID_ORDER_DATE);
        }
    }

    private Pageable createPageable(int page, int size) {
        int safePage = Math.max(page, DEFAULT_PAGE);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        return PageRequest.of(safePage, safeSize);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
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
