package s_map.server.domain.plan.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import s_map.server.domain.order.entity.CustomerOrder;
import s_map.server.domain.order.entity.OrderStatus;
import s_map.server.domain.order.entity.PlanStatus;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.order.repository.CustomerOrderRepository;
import s_map.server.domain.order.repository.ProductionPlanRepository;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateRequest;
import s_map.server.domain.plan.dto.fastapi.FastApiPlanningGenerateResponse;
import s_map.server.domain.plan.dto.req.PlanAiGenerateRequest;
import s_map.server.domain.plan.dto.req.PlanAiMonthlyGenerateRequest;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanAiServiceTest {

    @Mock
    private PlanningFastApiClient planningFastApiClient;

    @Mock
    private ProductionPlanRepository productionPlanRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @InjectMocks
    private PlanAiService planAiService;

    @Test
    @DisplayName("드래그 앤 드롭 AI 생성 요청은 생산계획 상태 기준으로 FastAPI 요청 body를 조립한다")
    void generatePlanningBuildsFastApiRequestFromPlanAndOrderData() {
        PlanAiGenerateRequest request = planAiGenerateRequest();
        ProductionPlan targetPlan = productionPlan(
                100L,
                421L,
                10L,
                PlanStatus.DELAYED,
                BigDecimal.valueOf(18),
                18_700
        );
        ProductionPlan candidatePlan = productionPlan(
                101L,
                422L,
                11L,
                PlanStatus.SCHEDULED,
                BigDecimal.valueOf(10),
                9_000
        );
        ProductionPlan previousPlan = productionPlan(
                99L,
                420L,
                9L,
                PlanStatus.SCHEDULED,
                BigDecimal.valueOf(5),
                4_000,
                OffsetDateTime.of(2026, 6, 5, 6, 0, 0, 0, ZoneOffset.ofHours(9)),
                OffsetDateTime.of(2026, 6, 5, 8, 0, 0, 0, ZoneOffset.ofHours(9))
        );
        ProductionPlan nextPlan = productionPlan(
                104L,
                425L,
                14L,
                PlanStatus.SCHEDULED,
                BigDecimal.valueOf(7),
                6_000,
                OffsetDateTime.of(2026, 6, 5, 18, 0, 0, 0, ZoneOffset.ofHours(9)),
                OffsetDateTime.of(2026, 6, 5, 20, 0, 0, 0, ZoneOffset.ofHours(9))
        );
        ProductionPlan completedPlan = productionPlan(
                102L,
                423L,
                12L,
                PlanStatus.COMPLETED,
                BigDecimal.valueOf(8),
                7_000
        );
        ProductionPlan cancelledPlan = productionPlan(
                103L,
                424L,
                13L,
                PlanStatus.CANCELLED,
                BigDecimal.valueOf(6),
                5_000
        );
        CustomerOrder targetOrder = customerOrder(
                421L,
                10L,
                LocalDate.of(2026, 6, 20),
                BigDecimal.valueOf(30_000_000),
                BigDecimal.valueOf(800_000),
                OrderStatus.COMPLETED
        );
        CustomerOrder candidateOrder = customerOrder(
                422L,
                11L,
                LocalDate.of(2026, 6, 15),
                null,
                BigDecimal.valueOf(120_000),
                OrderStatus.COMPLETED
        );
        CustomerOrder previousOrder = customerOrder(
                420L,
                9L,
                LocalDate.of(2026, 6, 14),
                BigDecimal.valueOf(10_000_000),
                BigDecimal.valueOf(100_000),
                OrderStatus.COMPLETED
        );
        CustomerOrder nextOrder = customerOrder(
                425L,
                14L,
                LocalDate.of(2026, 6, 16),
                BigDecimal.valueOf(12_000_000),
                BigDecimal.valueOf(150_000),
                OrderStatus.COMPLETED
        );

        when(productionPlanRepository.findById(100L)).thenReturn(Optional.of(targetPlan));
        when(productionPlanRepository.existsActiveLineCapability(2L, 10L)).thenReturn(true);
        when(productionPlanRepository.findByLineIdAndPlanIdNotAndPlannedStartAtLessThanAndPlannedEndAtGreaterThanOrderByPlannedStartAtAsc(
                2L,
                100L,
                request.getPlannedEndAt(),
                request.getPlannedStartAt()
        )).thenReturn(List.of(candidatePlan, completedPlan, cancelledPlan));
        when(productionPlanRepository.findPreviousLocalReplanningCandidate(
                2L,
                100L,
                List.of("COMPLETED", "CANCELLED", "IN_PROGRESS"),
                OffsetDateTime.of(2026, 6, 5, 9, 0, 0, 0, ZoneOffset.ofHours(9))
        )).thenReturn(Optional.of(previousPlan));
        when(productionPlanRepository.findNextLocalReplanningCandidate(
                2L,
                100L,
                List.of("COMPLETED", "CANCELLED", "IN_PROGRESS"),
                OffsetDateTime.of(2026, 6, 5, 17, 0, 0, 0, ZoneOffset.ofHours(9))
        )).thenReturn(Optional.of(nextPlan));
        when(customerOrderRepository.findAllById(any()))
                .thenReturn(List.of(targetOrder, candidateOrder, previousOrder, nextOrder));
        when(planningFastApiClient.generatePlanning(
                any(FastApiPlanningGenerateRequest.class),
                eq("Bearer access-token"),
                eq("refresh-token")
        )).thenReturn(new FastApiPlanningGenerateResponse());

        planAiService.generatePlanning(request, "Bearer access-token", "refresh-token");

        ArgumentCaptor<FastApiPlanningGenerateRequest> captor =
                ArgumentCaptor.forClass(FastApiPlanningGenerateRequest.class);
        verify(planningFastApiClient).generatePlanning(
                captor.capture(),
                eq("Bearer access-token"),
                eq("refresh-token")
        );

        FastApiPlanningGenerateRequest fastApiRequest = captor.getValue();
        assertThat(fastApiRequest.getPlanningStart()).isEqualTo("2026-06-05 06:00:00.000 +0900");
        assertThat(fastApiRequest.getPlanningEnd()).isEqualTo("2026-06-05 20:00:00.000 +0900");
        assertThat(fastApiRequest.getEditOrders()).hasSize(1);
        assertThat(fastApiRequest.getAddOrders()).hasSize(3);

        FastApiPlanningGenerateRequest.PlanningEditOrder editOrder =
                fastApiRequest.getEditOrders().getFirst();
        assertThat(editOrder.getOrderId()).isEqualTo(100L);
        assertThat(editOrder.getProductId()).isEqualTo(10L);
        assertThat(editOrder.getOrderQuantity()).isEqualTo(18_700);
        assertThat(editOrder.getDueDate()).isEqualTo("2026-06-20 08:59:59.000 +0900");
        assertThat(editOrder.getContractAmount()).isEqualByComparingTo("30000000");
        assertThat(editOrder.getLatePenaltyAmount()).isEqualByComparingTo("800000");
        assertThat(editOrder.getOrderStatus()).isEqualTo("DELAYED");
        assertThat(editOrder.getLockedPlan().getLineId()).isEqualTo(2L);
        assertThat(editOrder.getLockedPlan().getPlannedStartAt()).isEqualTo("2026-06-05 09:00:00.000 +0900");
        assertThat(editOrder.getLockedPlan().getPlannedEndAt()).isEqualTo("2026-06-05 17:00:00.000 +0900");

        assertThat(fastApiRequest.getAddOrders())
                .extracting(FastApiPlanningGenerateRequest.PlanningAddOrder::getOrderId)
                .containsExactly(99L, 101L, 104L);

        FastApiPlanningGenerateRequest.PlanningAddOrder addOrder =
                fastApiRequest.getAddOrders()
                        .stream()
                        .filter(order -> order.getOrderId().equals(101L))
                        .findFirst()
                        .orElseThrow();
        assertThat(addOrder.getOrderId()).isEqualTo(101L);
        assertThat(addOrder.getProductId()).isEqualTo(11L);
        assertThat(addOrder.getOrderQuantity()).isEqualTo(9_000);
        assertThat(addOrder.getDueDate()).isEqualTo("2026-06-15 08:59:59.000 +0900");
        assertThat(addOrder.getContractAmount()).isEqualByComparingTo("0");
        assertThat(addOrder.getLatePenaltyAmount()).isEqualByComparingTo("120000");
        assertThat(addOrder.getOrderStatus()).isEqualTo("SCHEDULED");
    }

    @Test
    @DisplayName("진행 중 생산계획은 FastAPI 조정 요청 대상에서 제외한다")
    void generatePlanningRejectsInProgressTargetPlan() {
        PlanAiGenerateRequest request = planAiGenerateRequest();
        ProductionPlan targetPlan = productionPlan(
                100L,
                421L,
                10L,
                PlanStatus.IN_PROGRESS,
                BigDecimal.valueOf(18),
                18_700
        );

        when(productionPlanRepository.findById(100L)).thenReturn(Optional.of(targetPlan));

        assertThatThrownBy(() -> planAiService.generatePlanning(
                request,
                "Bearer access-token",
                "refresh-token"
        ))
                .isInstanceOf(CustomException.class)
                .hasMessage("진행 중, 완료, 취소 상태의 생산계획은 AI 조정 요청 대상이 아닙니다.")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BAD_REQUEST);

        verify(planningFastApiClient, never()).generatePlanning(any(), any(), any());
    }

    @Test
    @DisplayName("월간 AI 분석 요청은 기간만 전달하고 edit/add 주문은 비워서 FastAPI 요청을 조립한다")
    void generateMonthlyPlanningBuildsFastApiRequestWithEmptyEditAndAddOrders() {
        PlanAiMonthlyGenerateRequest request = planAiMonthlyGenerateRequest();
        when(planningFastApiClient.generatePlanning(
                any(FastApiPlanningGenerateRequest.class),
                eq("Bearer access-token"),
                eq("refresh-token")
        )).thenReturn(new FastApiPlanningGenerateResponse());

        planAiService.generateMonthlyPlanning(request, "Bearer access-token", "refresh-token");

        ArgumentCaptor<FastApiPlanningGenerateRequest> captor =
                ArgumentCaptor.forClass(FastApiPlanningGenerateRequest.class);
        verify(planningFastApiClient).generatePlanning(
                captor.capture(),
                eq("Bearer access-token"),
                eq("refresh-token")
        );

        FastApiPlanningGenerateRequest fastApiRequest = captor.getValue();
        assertThat(fastApiRequest.getPlanningStart()).isEqualTo("2026-06-01 00:00:00.000 +0900");
        assertThat(fastApiRequest.getPlanningEnd()).isEqualTo("2026-07-01 00:00:00.000 +0900");
        assertThat(fastApiRequest.getEditOrders()).isEmpty();
        assertThat(fastApiRequest.getAddOrders()).isEmpty();
    }

    @Test
    @DisplayName("월간 AI 분석은 완료 또는 진행 중 계획의 마지막 종료 이후부터 재계획한다")
    void generateMonthlyPlanningStartsAfterLatestImmutablePlan() {
        PlanAiMonthlyGenerateRequest request = planAiMonthlyGenerateRequest();
        Instant latestImmutablePlanEnd = Instant.parse("2026-06-22T01:38:00Z");
        when(productionPlanRepository.findLatestImmutablePlanEnd(
                request.getPlanningStart(),
                request.getPlanningEnd(),
                List.of("COMPLETED", "IN_PROGRESS")
        )).thenReturn(Optional.of(latestImmutablePlanEnd));
        when(planningFastApiClient.generatePlanning(
                any(FastApiPlanningGenerateRequest.class),
                eq("Bearer access-token"),
                eq("refresh-token")
        )).thenReturn(new FastApiPlanningGenerateResponse());

        planAiService.generateMonthlyPlanning(request, "Bearer access-token", "refresh-token");

        ArgumentCaptor<FastApiPlanningGenerateRequest> captor =
                ArgumentCaptor.forClass(FastApiPlanningGenerateRequest.class);
        verify(planningFastApiClient).generatePlanning(
                captor.capture(),
                eq("Bearer access-token"),
                eq("refresh-token")
        );
        assertThat(captor.getValue().getPlanningStart())
                .isEqualTo("2026-06-22 10:38:00.000 +0900");
    }

    private PlanAiGenerateRequest planAiGenerateRequest() {
        PlanAiGenerateRequest request = new PlanAiGenerateRequest();
        ReflectionTestUtils.setField(request, "planId", 100L);
        ReflectionTestUtils.setField(request, "lineId", 2L);
        ReflectionTestUtils.setField(
                request,
                "plannedStartAt",
                OffsetDateTime.of(2026, 6, 5, 9, 0, 0, 0, ZoneOffset.ofHours(9))
        );
        ReflectionTestUtils.setField(
                request,
                "plannedEndAt",
                OffsetDateTime.of(2026, 6, 5, 17, 0, 0, 0, ZoneOffset.ofHours(9))
        );
        ReflectionTestUtils.setField(
                request,
                "planningStart",
                OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.ofHours(9))
        );
        ReflectionTestUtils.setField(
                request,
                "planningEnd",
                OffsetDateTime.of(2026, 6, 30, 23, 59, 59, 0, ZoneOffset.ofHours(9))
        );
        return request;
    }

    private PlanAiMonthlyGenerateRequest planAiMonthlyGenerateRequest() {
        PlanAiMonthlyGenerateRequest request = new PlanAiMonthlyGenerateRequest();
        ReflectionTestUtils.setField(
                request,
                "planningStart",
                OffsetDateTime.of(2026, 6, 1, 0, 0, 0, 0, ZoneOffset.ofHours(9))
        );
        ReflectionTestUtils.setField(
                request,
                "planningEnd",
                OffsetDateTime.of(2026, 7, 1, 0, 0, 0, 0, ZoneOffset.ofHours(9))
        );
        return request;
    }

    private ProductionPlan productionPlan(
            Long planId,
            Long orderId,
            Long productId,
            PlanStatus planStatus,
            BigDecimal estimatedDurationHr,
            Integer plannedQuantity
    ) {
        return productionPlan(
                planId,
                orderId,
                productId,
                planStatus,
                estimatedDurationHr,
                plannedQuantity,
                OffsetDateTime.of(2026, 6, 5, 9, 0, 0, 0, ZoneOffset.ofHours(9)),
                OffsetDateTime.of(2026, 6, 5, 17, 0, 0, 0, ZoneOffset.ofHours(9))
        );
    }

    private ProductionPlan productionPlan(
            Long planId,
            Long orderId,
            Long productId,
            PlanStatus planStatus,
            BigDecimal estimatedDurationHr,
            Integer plannedQuantity,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt
    ) {
        ProductionPlan plan = ProductionPlan.create(
                orderId,
                productId,
                1L,
                3L,
                plannedStartAt,
                plannedEndAt,
                estimatedDurationHr,
                plannedQuantity,
                1
        );
        ReflectionTestUtils.setField(plan, "planId", planId);
        ReflectionTestUtils.setField(plan, "planStatus", planStatus);
        return plan;
    }

    private CustomerOrder customerOrder(
            Long orderId,
            Long productId,
            LocalDate dueDate,
            BigDecimal contractAmount,
            BigDecimal latePenaltyAmount,
            OrderStatus orderStatus
    ) {
        CustomerOrder order = CustomerOrder.create(
                "PO-" + orderId,
                productId,
                500,
                "A사",
                "김담당",
                LocalDate.of(2026, 6, 1),
                dueDate,
                contractAmount,
                latePenaltyAmount
        );
        ReflectionTestUtils.setField(order, "orderId", orderId);
        ReflectionTestUtils.setField(order, "orderStatus", orderStatus);
        return order;
    }
}
