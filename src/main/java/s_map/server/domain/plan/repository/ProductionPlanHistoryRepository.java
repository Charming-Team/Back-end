package s_map.server.domain.plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import s_map.server.domain.plan.entity.ProductionPlanHistory;

import java.util.List;

public interface ProductionPlanHistoryRepository extends JpaRepository<ProductionPlanHistory, Long> {

    List<ProductionPlanHistory> findByRollbackSnapshotIdOrderByPlanHistoryIdAsc(String rollbackSnapshotId);
}
