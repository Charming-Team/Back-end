package s_map.server.domain.plan.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import s_map.server.domain.line.entity.ProductionLine;
import s_map.server.domain.line.repository.ProductionLineRepository;
import s_map.server.domain.order.entity.CustomerOrder;
import s_map.server.domain.order.entity.PlanStatus;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.order.repository.CustomerOrderRepository;
import s_map.server.domain.order.repository.ProductionPlanRepository;
import s_map.server.domain.plan.dto.req.PlanFileApplyMode;
import s_map.server.domain.plan.dto.res.PlanFileApplyResponse;
import s_map.server.domain.plan.entity.PlanFileApplyHistory;
import s_map.server.domain.plan.entity.ProductionPlanHistory;
import s_map.server.domain.plan.repository.PlanFileApplyHistoryRepository;
import s_map.server.domain.plan.repository.ProductionPlanHistoryRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
class PlanFileServiceIntegrationTest {

    @Autowired
    private PlanFileService planFileService;

    @Autowired
    private ProductionPlanRepository productionPlanRepository;

    @Autowired
    private CustomerOrderRepository customerOrderRepository;

    @Autowired
    private ProductionLineRepository productionLineRepository;

    @Autowired
    private PlanFileApplyHistoryRepository planFileApplyHistoryRepository;

    @Autowired
    private ProductionPlanHistoryRepository productionPlanHistoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        createReferenceTables();
        productionPlanHistoryRepository.deleteAll();
        planFileApplyHistoryRepository.deleteAll();
        productionPlanRepository.deleteAll();
        customerOrderRepository.deleteAll();
        productionLineRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM product_line_capabilities");
        jdbcTemplate.update("DELETE FROM products");
    }

    @Test
    @DisplayName("FULL_REPLACE는 기존 운영 계획을 이력으로 백업하고 운영 해제한 뒤 동일 순번 신규 계획을 저장한다")
    void fullReplaceBacksUpAndArchivesCurrentPlansBeforeInsert() {
        jdbcTemplate.update(
                "INSERT INTO products (product_id, product_code, product_name) VALUES (?, ?, ?)",
                1L,
                "P-001",
                "브레이크 패드"
        );
        ProductionLine line = productionLineRepository.save(ProductionLine.builder()
                .lineCode("LINE-A")
                .lineName("A라인")
                .maxCapacityPerDay(100)
                .capacityUnit("EA")
                .supportsChangeover(true)
                .active(true)
                .description("테스트 라인")
                .build());
        jdbcTemplate.update(
                """
                INSERT INTO product_line_capabilities
                    (product_id, line_id, capacity_per_day, standard_production_time_hr, priority_rank)
                VALUES (?, ?, ?, ?, ?)
                """,
                1L,
                line.getLineId(),
                100,
                BigDecimal.ONE,
                1
        );

        CustomerOrder order = customerOrderRepository.save(CustomerOrder.create(
                "ORD-001",
                1L,
                10,
                "A사",
                "홍길동",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                null,
                null
        ));
        ProductionPlan existingPlan = productionPlanRepository.saveAndFlush(ProductionPlan.create(
                order.getOrderId(),
                1L,
                line.getLineId(),
                null,
                OffsetDateTime.parse("2026-06-10T09:00:00+09:00"),
                OffsetDateTime.parse("2026-06-10T11:00:00+09:00"),
                BigDecimal.valueOf(2),
                10,
                1
        ));
        Long existingPlanId = existingPlan.getPlanId();

        PlanFileApplyResponse response = planFileService.applyPlanFile(
                csvFile("""
                        order_id,product_id,line_id,planned_start_at,planned_end_at,planned_quantity,plan_sequence
                        %d,1,%d,2026-06-11T09:00:00+09:00,2026-06-11T11:00:00+09:00,10,1
                        """.formatted(order.getOrderId(), line.getLineId())),
                PlanFileApplyMode.FULL_REPLACE
        );

        Assertions.assertTrue(response.isApplied());
        Assertions.assertEquals(1, response.getAppliedRows());
        Assertions.assertNotNull(response.getApplyHistoryId());
        Assertions.assertNotNull(response.getRollbackSnapshotId());

        ProductionPlan archivedPlan = productionPlanRepository.findById(existingPlanId).orElseThrow();
        Assertions.assertFalse(archivedPlan.isCurrent());
        Assertions.assertEquals(PlanStatus.CANCELLED, archivedPlan.getPlanStatus());
        Assertions.assertEquals(-existingPlanId.intValue(), archivedPlan.getPlanSequence());

        List<ProductionPlan> currentPlans = productionPlanRepository.findAll()
                .stream()
                .filter(ProductionPlan::isCurrent)
                .filter(plan -> plan.getPlanStatus() == PlanStatus.SCHEDULED)
                .toList();
        Assertions.assertEquals(1, currentPlans.size());
        Assertions.assertNotEquals(existingPlanId, currentPlans.getFirst().getPlanId());
        Assertions.assertEquals(1, currentPlans.getFirst().getPlanSequence());

        PlanFileApplyHistory applyHistory = planFileApplyHistoryRepository
                .findById(response.getApplyHistoryId())
                .orElseThrow();
        Assertions.assertTrue(applyHistory.isApplied());
        Assertions.assertEquals(1, applyHistory.getBackedUpRows());
        Assertions.assertEquals(1, applyHistory.getAppliedRows());
        Assertions.assertEquals(response.getRollbackSnapshotId(), applyHistory.getRollbackSnapshotId());

        List<ProductionPlanHistory> snapshots = productionPlanHistoryRepository
                .findByRollbackSnapshotIdOrderByPlanHistoryIdAsc(response.getRollbackSnapshotId());
        Assertions.assertEquals(1, snapshots.size());
        ProductionPlanHistory snapshot = snapshots.getFirst();
        Assertions.assertEquals(existingPlanId, snapshot.getSourcePlanId());
        Assertions.assertEquals(1, snapshot.getPlanSequence());
        Assertions.assertEquals("SCHEDULED", snapshot.getPlanStatus());
        Assertions.assertTrue(snapshot.isSourceCurrent());
    }

    private MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file",
                "plans.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void createReferenceTables() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    product_id BIGINT PRIMARY KEY,
                    product_code VARCHAR(50) NOT NULL,
                    product_name VARCHAR(100) NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS product_line_capabilities (
                    product_id BIGINT NOT NULL,
                    line_id BIGINT NOT NULL,
                    capacity_per_day INTEGER,
                    standard_production_time_hr NUMERIC(10, 2),
                    priority_rank INTEGER
                )
                """);
    }
}
