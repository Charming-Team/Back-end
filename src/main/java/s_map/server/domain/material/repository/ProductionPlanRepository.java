package s_map.server.domain.material.repository;

import s_map.server.domain.material.entity.PlanStatus;
import s_map.server.domain.material.entity.ProductionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProductionPlanRepository extends JpaRepository<ProductionPlan, Long> {

    List<ProductionPlan> findByProductId(Long productId);

    List<ProductionPlan> findByLineId(Long lineId);

    List<ProductionPlan> findByPlanStatus(PlanStatus planStatus);

    List<ProductionPlan> findByPlannedStartAtBetween(
            LocalDateTime startAt,
            LocalDateTime endAt
    );

    List<ProductionPlan> findByPlanStatusIn(List<PlanStatus> planStatuses);
}