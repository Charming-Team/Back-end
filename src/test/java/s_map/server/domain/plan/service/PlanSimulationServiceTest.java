package s_map.server.domain.plan.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import s_map.server.domain.order.entity.CustomerOrder;
import s_map.server.domain.order.entity.PlanStatus;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.order.repository.CustomerOrderRepository;
import s_map.server.domain.order.repository.ProductionPlanRepository;
import s_map.server.domain.plan.dto.req.SelectedPlanSimulationSaveRequest;
import s_map.server.domain.plan.repository.PlanSimulationCommandRepository;
import s_map.server.domain.plan.repository.PlanSimulationRepository;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.UserStatus;
import s_map.server.domain.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanSimulationServiceTest {

    @Mock
    private PlanSimulationRepository planSimulationRepository;

    @Mock
    private PlanSimulationCommandRepository planSimulationCommandRepository;

    @Mock
    private ProductionPlanRepository productionPlanRepository;

    @Mock
    private CustomerOrderRepository customerOrderRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PlanSimulationService planSimulationService;

    @Test
    @DisplayName("선택 대안 저장 시 기존 주문의 생산계획은 신규 생성하지 않고 갱신한다")
    void saveSelectedSimulationUpdatesExistingPlan() {
        OffsetDateTime startAt = OffsetDateTime.of(2026, 6, 10, 9, 0, 0, 0, ZoneOffset.ofHours(9));
        SelectedPlanSimulationSaveRequest request = selectedSimulationSaveRequest(startAt);
        SelectedPlanSimulationSaveRequest.SelectedPlan selectedPlan = request.getPlans().getFirst();
        CustomerOrder order = customerOrder(100L, 10L);
        ProductionPlan existingPlan = productionPlan(200L, 100L, 10L);

        when(customerOrderRepository.findAllById(any())).thenReturn(List.of(order));
        when(productionPlanRepository.findByOrderIdInAndPlanStatusNot(anyList(), eq(PlanStatus.CANCELLED)))
                .thenReturn(List.of(existingPlan));
        when(productionPlanRepository.existsActiveLineCapability(2L, 10L)).thenReturn(true);
        when(userRepository.existsByIdAndStatusAndRole(7L, UserStatus.ACTIVE, Role.OPERATOR)).thenReturn(true);
        when(productionPlanRepository.existsLineSequenceOutsidePlans(2L, 5, List.of(200L))).thenReturn(false);
        when(productionPlanRepository.existsScheduleConflictOutsidePlans(
                eq(2L),
                eq(startAt),
                eq(startAt.plusHours(4)),
                eq(PlanStatus.CANCELLED),
                eq(List.of(200L))
        )).thenReturn(false);
        when(planSimulationCommandRepository.saveSimulationResult(
                eq(request),
                anyString(),
                eq("DUE_DATE_OPTIMIZATION"),
                eq(9L),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(300L);
        when(productionPlanRepository.findMaxPlanSequence()).thenReturn(10);

        var response = planSimulationService.saveSelectedSimulation(request, 9L);

        assertThat(response.getSavedPlanIds()).containsExactly(200L);
        assertThat(existingPlan.getLineId()).isEqualTo(2L);
        assertThat(existingPlan.getOperatorId()).isEqualTo(7L);
        assertThat(existingPlan.getPlannedStartAt()).isEqualTo(startAt);
        assertThat(existingPlan.getPlannedEndAt()).isEqualTo(startAt.plusHours(4));
        assertThat(existingPlan.getPlannedQuantity()).isEqualTo(500);
        assertThat(existingPlan.getPlanSequence()).isEqualTo(5);
        assertThat(existingPlan.getPlanStatus()).isEqualTo(PlanStatus.SCHEDULED);

        verify(productionPlanRepository).flush();
        verify(productionPlanRepository, never()).save(any(ProductionPlan.class));
        verify(planSimulationCommandRepository).saveSimulationDetail(
                eq(300L),
                eq(200L),
                eq(selectedPlan),
                any(OffsetDateTime.class)
        );
    }

    @Test
    @DisplayName("기존안을 선택하면 생산계획을 수정하지 않는다")
    void saveSelectedSimulationDoesNotApplyBaselineSelection() {
        SelectedPlanSimulationSaveRequest request = baselineSelectionRequest();

        when(planSimulationCommandRepository.saveSimulationResult(
                eq(request),
                anyString(),
                eq("CURRENT_PLAN_BASELINE"),
                eq(null),
                eq(null),
                any(OffsetDateTime.class)
        )).thenReturn(301L);

        var response = planSimulationService.saveSelectedSimulation(request, 9L);

        assertThat(response.isApplied()).isFalse();
        assertThat(response.getSavedPlanCount()).isZero();
        assertThat(response.getSavedDetailCount()).isZero();
        assertThat(response.getSavedPlanIds()).isEmpty();
        assertThat(response.getAppliedBy()).isNull();
        assertThat(response.getAppliedAt()).isNull();

        verify(customerOrderRepository, never()).findAllById(any());
        verify(productionPlanRepository, never()).save(any(ProductionPlan.class));
        verify(productionPlanRepository, never()).flush();
    }

    private SelectedPlanSimulationSaveRequest selectedSimulationSaveRequest(OffsetDateTime startAt) {
        SelectedPlanSimulationSaveRequest request = new SelectedPlanSimulationSaveRequest();
        ReflectionTestUtils.setField(request, "simulationName", "납기 최적화 대안");
        ReflectionTestUtils.setField(request, "planVariantCode", "DUE_DATE_OPTIMAL");
        ReflectionTestUtils.setField(request, "beforeTotalDelayHr", BigDecimal.valueOf(12));
        ReflectionTestUtils.setField(request, "afterTotalDelayHr", BigDecimal.valueOf(3));
        ReflectionTestUtils.setField(request, "plans", List.of(selectedPlan(startAt)));
        return request;
    }

    private SelectedPlanSimulationSaveRequest baselineSelectionRequest() {
        SelectedPlanSimulationSaveRequest request = new SelectedPlanSimulationSaveRequest();
        ReflectionTestUtils.setField(request, "simulationName", "현재 계획 유지");
        ReflectionTestUtils.setField(request, "planVariantCode", "CURRENT_PLAN_BASELINE");
        ReflectionTestUtils.setField(request, "beforeTotalDelayHr", BigDecimal.valueOf(12));
        ReflectionTestUtils.setField(request, "afterTotalDelayHr", BigDecimal.valueOf(12));
        ReflectionTestUtils.setField(
                request,
                "plans",
                List.of(selectedPlan(OffsetDateTime.of(2026, 6, 10, 9, 0, 0, 0, ZoneOffset.ofHours(9))))
        );
        return request;
    }

    private SelectedPlanSimulationSaveRequest.SelectedPlan selectedPlan(OffsetDateTime startAt) {
        SelectedPlanSimulationSaveRequest.SelectedPlan plan = new SelectedPlanSimulationSaveRequest.SelectedPlan();
        ReflectionTestUtils.setField(plan, "orderId", 100L);
        ReflectionTestUtils.setField(plan, "productId", 10L);
        ReflectionTestUtils.setField(plan, "lineId", 2L);
        ReflectionTestUtils.setField(plan, "operatorId", 7L);
        ReflectionTestUtils.setField(plan, "plannedStartAt", startAt);
        ReflectionTestUtils.setField(plan, "plannedEndAt", startAt.plusHours(4));
        ReflectionTestUtils.setField(plan, "estimatedDurationHr", BigDecimal.valueOf(4));
        ReflectionTestUtils.setField(plan, "plannedQuantity", 500);
        ReflectionTestUtils.setField(plan, "planSequence", 5);
        ReflectionTestUtils.setField(plan, "afterDelayed", false);
        return plan;
    }

    private CustomerOrder customerOrder(Long orderId, Long productId) {
        CustomerOrder order = CustomerOrder.create(
                "PO-260610-001",
                productId,
                500,
                "A사",
                "김담당",
                LocalDate.of(2026, 6, 9),
                LocalDate.of(2026, 6, 20),
                BigDecimal.valueOf(1_000_000),
                BigDecimal.valueOf(50_000)
        );
        ReflectionTestUtils.setField(order, "orderId", orderId);
        return order;
    }

    private ProductionPlan productionPlan(Long planId, Long orderId, Long productId) {
        ProductionPlan plan = ProductionPlan.create(
                orderId,
                productId,
                1L,
                3L,
                OffsetDateTime.of(2026, 6, 9, 9, 0, 0, 0, ZoneOffset.ofHours(9)),
                OffsetDateTime.of(2026, 6, 9, 13, 0, 0, 0, ZoneOffset.ofHours(9)),
                BigDecimal.valueOf(4),
                500,
                1
        );
        ReflectionTestUtils.setField(plan, "planId", planId);
        return plan;
    }
}
