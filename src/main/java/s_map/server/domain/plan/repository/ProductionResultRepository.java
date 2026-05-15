package s_map.server.domain.plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import s_map.server.domain.plan.entity.ProductionResult;

import java.util.List;
import java.util.Optional;

public interface ProductionResultRepository extends JpaRepository<ProductionResult, Long> {

    Optional<ProductionResult> findByPlanId(Long planId);

    List<ProductionResult> findByPlanIdIn(List<Long> planIds);
}