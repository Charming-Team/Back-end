package s_map.server.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import s_map.server.domain.report.entity.ReportJob;

public interface ReportJobRepository extends JpaRepository<ReportJob, Long> {
}