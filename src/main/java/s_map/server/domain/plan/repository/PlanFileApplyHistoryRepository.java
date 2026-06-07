package s_map.server.domain.plan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import s_map.server.domain.plan.entity.PlanFileApplyHistory;

public interface PlanFileApplyHistoryRepository extends JpaRepository<PlanFileApplyHistory, Long> {
}
