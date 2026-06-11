package s_map.server.domain.order.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import s_map.server.domain.order.dto.req.OrderCreateRequest;
import s_map.server.domain.order.entity.CustomerOrder;
import s_map.server.domain.order.entity.OrderStatus;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.order.repository.CustomerOrderRepository;
import s_map.server.domain.order.repository.LineAssignmentCandidateProjection;
import s_map.server.domain.order.repository.OrderNoSequenceRepository;
import s_map.server.domain.order.repository.OrderQueryRepository;
import s_map.server.domain.order.repository.OrderSummaryProjection;
import s_map.server.domain.order.repository.ProductQueryRepository;
import s_map.server.domain.order.repository.ProductionPlanRepository;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.UserStatus;
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final DateTimeFormatter ORDER_NO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");
    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalDate MIN_DUE_DATE_FILTER = LocalDate.of(1, 1, 1);
    private static final LocalDate MAX_DUE_DATE_FILTER = LocalDate.of(9999, 12, 31);

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private OrderQueryRepository orderQueryRepository;

    @Mock
    private ProductQueryRepository productQueryRepository;

    @Mock
    private ProductionPlanRepository productionPlanRepository;

    @Mock
    private OrderNoSequenceRepository orderNoSequenceRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("주문 등록 시 가장 빨리 종료되는 라인에 생산계획을 생성한다")
    void createOrderAssignsEarliestEndingLine() {
        OffsetDateTime desiredStartAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        LocalDate dueDate = desiredStartAt.plusDays(5).toLocalDate();
        OrderCreateRequest request = createRequest(dueDate, desiredStartAt, 3L);

        when(productQueryRepository.findProductNameById(1L)).thenReturn(Optional.of("ABS-Black"));
        when(userRepository.existsByIdAndStatusAndRole(3L, UserStatus.ACTIVE, Role.OPERATOR)).thenReturn(true);
        when(productionPlanRepository.lockAssignableLineIds(1L)).thenReturn(List.of(10L, 20L));
        when(productionPlanRepository.findAssignmentCandidates(1L)).thenReturn(List.of(
                candidate(10L, "느린 라인", 100, null, 1, null, 2),
                candidate(20L, "빠른 라인", 500, null, 2, null, 4)
        ));
        when(customerOrderRepository.findLatestOrderNoByPrefix(anyString())).thenReturn(Optional.empty());
        when(orderNoSequenceRepository.nextSequence(any(LocalDate.class), eq(1))).thenReturn(7);
        when(customerOrderRepository.saveAndFlush(any(CustomerOrder.class))).thenAnswer(invocation -> {
            CustomerOrder order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "orderId", 101L);
            return order;
        });
        when(productionPlanRepository.saveAndFlush(any(ProductionPlan.class))).thenAnswer(invocation -> {
            ProductionPlan plan = invocation.getArgument(0);
            ReflectionTestUtils.setField(plan, "planId", 201L);
            return plan;
        });

        var response = orderService.createOrder(request);

        ArgumentCaptor<CustomerOrder> orderCaptor = ArgumentCaptor.forClass(CustomerOrder.class);
        ArgumentCaptor<ProductionPlan> planCaptor = ArgumentCaptor.forClass(ProductionPlan.class);
        verify(customerOrderRepository).saveAndFlush(orderCaptor.capture());
        verify(productionPlanRepository).saveAndFlush(planCaptor.capture());

        CustomerOrder savedOrder = orderCaptor.getValue();
        ProductionPlan savedPlan = planCaptor.getValue();

        assertThat(savedOrder.getOrderNo())
                .isEqualTo("PO-" + LocalDate.now(DEFAULT_PRODUCTION_ZONE).format(ORDER_NO_DATE_FORMATTER) + "-007");
        assertThat(savedOrder.getCustomerName()).isEqualTo("A사");
        assertThat(savedOrder.getOrderQuantity()).isEqualTo(1000);
        assertThat(savedPlan.getOrderId()).isEqualTo(101L);
        assertThat(savedPlan.getLineId()).isEqualTo(20L);
        assertThat(savedPlan.getOperatorId()).isEqualTo(3L);
        assertThat(savedPlan.getPlanSequence()).isEqualTo(5);
        assertThat(savedPlan.getEstimatedDurationHr()).isEqualByComparingTo("48.00");
        assertThat(savedPlan.getPlannedStartAt()).isEqualTo(desiredStartAt);
        assertThat(savedPlan.getPlannedEndAt()).isEqualTo(desiredStartAt.plusHours(48));
        assertThat(response.lineId()).isEqualTo(20L);
        assertThat(response.lineName()).isEqualTo("빠른 라인");
    }

    @Test
    @DisplayName("계획 종료일이 납기일을 넘으면 주문 등록을 거절한다")
    void createOrderFailsWhenPlanEndExceedsDueDate() {
        OffsetDateTime desiredStartAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        OrderCreateRequest request = createRequest(desiredStartAt.plusDays(1).toLocalDate(), desiredStartAt, 3L);

        when(productQueryRepository.findProductNameById(1L)).thenReturn(Optional.of("ABS-Black"));
        when(userRepository.existsByIdAndStatusAndRole(3L, UserStatus.ACTIVE, Role.OPERATOR)).thenReturn(true);
        when(productionPlanRepository.lockAssignableLineIds(1L)).thenReturn(List.of(10L));
        when(productionPlanRepository.findAssignmentCandidates(1L)).thenReturn(List.of(
                candidate(10L, "느린 라인", 100, null, 1, null, 1)
        ));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_SCHEDULE_EXCEEDS_DUE_DATE);

        verify(customerOrderRepository, never()).saveAndFlush(any(CustomerOrder.class));
        verify(productionPlanRepository, never()).saveAndFlush(any(ProductionPlan.class));
    }

    @Test
    @DisplayName("활성 OPERATOR가 아니면 생산 담당자로 지정할 수 없다")
    void createOrderFailsWhenOperatorIsNotActiveOperator() {
        OffsetDateTime desiredStartAt = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
        OrderCreateRequest request = createRequest(desiredStartAt.plusDays(5).toLocalDate(), desiredStartAt, 3L);

        when(productQueryRepository.findProductNameById(1L)).thenReturn(Optional.of("ABS-Black"));
        when(userRepository.existsByIdAndStatusAndRole(3L, UserStatus.ACTIVE, Role.OPERATOR)).thenReturn(false);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.OPERATOR_NOT_FOUND);

        verify(productionPlanRepository, never()).lockAssignableLineIds(any());
        verify(customerOrderRepository, never()).saveAndFlush(any(CustomerOrder.class));
        verify(productionPlanRepository, never()).saveAndFlush(any(ProductionPlan.class));
    }

    @Test
    @DisplayName("희망 생산 시작일시가 과거이면 주문 등록을 거절한다")
    void createOrderFailsWhenDesiredStartAtIsPast() {
        OffsetDateTime desiredStartAt = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1);
        OrderCreateRequest request = createRequest(LocalDate.now(DEFAULT_PRODUCTION_ZONE).plusDays(5), desiredStartAt, 3L);

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_ORDER_DATE);

        verify(productQueryRepository, never()).findProductNameById(any());
        verify(productionPlanRepository, never()).lockAssignableLineIds(any());
        verify(customerOrderRepository, never()).saveAndFlush(any(CustomerOrder.class));
        verify(productionPlanRepository, never()).saveAndFlush(any(ProductionPlan.class));
    }

    @Test
    @DisplayName("상태 필터가 없으면 경량 목록 쿼리와 단순 count 쿼리로 조회한다")
    void getOrdersWithoutStatusUsesPagedStatusQueryAndSimpleCount() {
        when(orderQueryRepository.findOrderSummariesWithoutStatusFilter(
                eq(null),
                eq(null),
                eq(null),
                eq(MIN_DUE_DATE_FILTER),
                eq(MAX_DUE_DATE_FILTER),
                eq(10),
                eq(0L),
                any(LocalDate.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of(summaryProjection()));
        when(orderQueryRepository.countOrderSummariesWithoutStatusFilter(
                eq(null),
                eq(null),
                eq(null),
                eq(MIN_DUE_DATE_FILTER),
                eq(MAX_DUE_DATE_FILTER)
        )).thenReturn(1L);

        var response = orderService.getOrders(0, 10, null, null, null, null, null, null);

        verify(orderQueryRepository).findOrderSummariesWithoutStatusFilter(
                eq(null),
                eq(null),
                eq(null),
                eq(MIN_DUE_DATE_FILTER),
                eq(MAX_DUE_DATE_FILTER),
                eq(10),
                eq(0L),
                any(LocalDate.class),
                any(OffsetDateTime.class)
        );
        verify(orderQueryRepository).countOrderSummariesWithoutStatusFilter(
                eq(null),
                eq(null),
                eq(null),
                eq(MIN_DUE_DATE_FILTER),
                eq(MAX_DUE_DATE_FILTER)
        );
        verify(orderQueryRepository, never()).findOrderSummaries(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(Pageable.class)
        );
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().orderStatusLabel()).isEqualTo("진행 중");
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("상태 필터가 있으면 상태 계산 포함 목록 쿼리로 조회한다")
    void getOrdersWithStatusUsesStatusFilterQuery() {
        when(orderQueryRepository.findOrderSummaries(
                eq(null),
                eq("IN_PROGRESS"),
                eq(null),
                eq(null),
                eq(MIN_DUE_DATE_FILTER),
                eq(MAX_DUE_DATE_FILTER),
                any(LocalDate.class),
                any(OffsetDateTime.class),
                any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(summaryProjection())));

        var response = orderService.getOrders(0, 10, null, OrderStatus.IN_PROGRESS, null, null, null, null);

        verify(orderQueryRepository).findOrderSummaries(
                eq(null),
                eq("IN_PROGRESS"),
                eq(null),
                eq(null),
                eq(MIN_DUE_DATE_FILTER),
                eq(MAX_DUE_DATE_FILTER),
                any(LocalDate.class),
                any(OffsetDateTime.class),
                any(Pageable.class)
        );
        verify(orderQueryRepository, never()).findOrderSummariesWithoutStatusFilter(
                any(),
                any(),
                any(),
                any(),
                any(),
                anyInt(),
                anyLong(),
                any(LocalDate.class),
                any(OffsetDateTime.class)
        );
        verify(orderQueryRepository, never()).countOrderSummariesWithoutStatusFilter(
                any(),
                any(),
                any(),
                any(),
                any()
        );
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().orderStatusLabel()).isEqualTo("진행 중");
    }

    @Test
    @DisplayName("한쪽 납기일 필터만 있어도 native query에는 null 날짜를 전달하지 않는다")
    void getOrdersWithOneSidedDueDateFilterUsesEffectiveDateRange() {
        LocalDate dueDateFrom = LocalDate.of(2026, 6, 1);

        when(orderQueryRepository.findOrderSummariesWithoutStatusFilter(
                eq(null),
                eq(null),
                eq(null),
                eq(dueDateFrom),
                eq(MAX_DUE_DATE_FILTER),
                eq(10),
                eq(0L),
                any(LocalDate.class),
                any(OffsetDateTime.class)
        )).thenReturn(List.of(summaryProjection()));
        when(orderQueryRepository.countOrderSummariesWithoutStatusFilter(
                eq(null),
                eq(null),
                eq(null),
                eq(dueDateFrom),
                eq(MAX_DUE_DATE_FILTER)
        )).thenReturn(1L);

        var response = orderService.getOrders(0, 10, null, null, null, null, dueDateFrom, null);

        verify(orderQueryRepository).findOrderSummariesWithoutStatusFilter(
                eq(null),
                eq(null),
                eq(null),
                eq(dueDateFrom),
                eq(MAX_DUE_DATE_FILTER),
                eq(10),
                eq(0L),
                any(LocalDate.class),
                any(OffsetDateTime.class)
        );
        verify(orderQueryRepository).countOrderSummariesWithoutStatusFilter(
                eq(null),
                eq(null),
                eq(null),
                eq(dueDateFrom),
                eq(MAX_DUE_DATE_FILTER)
        );
        assertThat(response.getContent()).hasSize(1);
    }

    private OrderCreateRequest createRequest(LocalDate dueDate, OffsetDateTime desiredStartAt, Long operatorId) {
        return new OrderCreateRequest(
                " A사 ",
                1L,
                1000,
                dueDate,
                null,
                desiredStartAt,
                operatorId,
                null,
                " 박고객 ",
                BigDecimal.valueOf(10_000_000),
                BigDecimal.valueOf(500_000)
        );
    }

    private LineAssignmentCandidateProjection candidate(
            Long lineId,
            String lineName,
            Integer capacityPerDay,
            BigDecimal standardProductionTimeHr,
            Integer priorityRank,
            OffsetDateTime lastPlannedEndAt,
            Integer lastPlanSequence
    ) {
        return new LineAssignmentCandidateProjection() {
            @Override
            public Long getLineId() {
                return lineId;
            }

            @Override
            public String getLineName() {
                return lineName;
            }

            @Override
            public Integer getCapacityPerDay() {
                return capacityPerDay;
            }

            @Override
            public BigDecimal getStandardProductionTimeHr() {
                return standardProductionTimeHr;
            }

            @Override
            public Integer getPriorityRank() {
                return priorityRank;
            }

            @Override
            public Instant getLastPlannedEndAt() {
                return lastPlannedEndAt == null ? null : lastPlannedEndAt.toInstant();
            }

            @Override
            public Integer getLastPlanSequence() {
                return lastPlanSequence;
            }
        };
    }

    private OrderSummaryProjection summaryProjection() {
        return new OrderSummaryProjection() {
            @Override
            public Long getOrderId() {
                return 1L;
            }

            @Override
            public String getOrderNo() {
                return "PO-260527-001";
            }

            @Override
            public String getCustomerName() {
                return "A사";
            }

            @Override
            public Long getProductId() {
                return 1L;
            }

            @Override
            public String getProductCode() {
                return "ABS-BLK";
            }

            @Override
            public String getProductName() {
                return "ABS-Black";
            }

            @Override
            public Integer getOrderQuantity() {
                return 1000;
            }

            @Override
            public LocalDate getDueDate() {
                return LocalDate.now(DEFAULT_PRODUCTION_ZONE).plusDays(7);
            }

            @Override
            public String getOrderStatus() {
                return "IN_PROGRESS";
            }
        };
    }
}
