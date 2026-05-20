package s_map.server.domain.plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import s_map.server.domain.plan.entity.PlanStatus;
import s_map.server.domain.plan.entity.ProductionPlan;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, Long> {

    List<ProductionPlan> findAllByOrderByPlannedStartAtAsc();

    List<ProductionPlan> findByPlanStatusInOrderByPlannedStartAtAsc(List<PlanStatus> planStatuses);

    List<ProductionPlan> findByPlannedStartAtLessThanEqualAndPlannedEndAtGreaterThanEqualOrderByPlannedStartAtAsc(
            LocalDateTime endOfDay,
            LocalDateTime startOfDay
    );

    List<ProductionPlan> findByLineIdOrderByPlannedStartAtAsc(Long lineId);

    List<ProductionPlan> findByProductIdOrderByPlannedStartAtAsc(Long productId);

    boolean existsByLineIdAndPlanIdNotAndPlannedStartAtLessThanAndPlannedEndAtGreaterThan(
            Long lineId,
            Long planId,
            LocalDateTime plannedEndAt,
            LocalDateTime plannedStartAt
    );
}