package s_map.server.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import s_map.server.domain.report.entity.ReportJob;
import s_map.server.domain.report.entity.ReportJobStatus;

import java.util.Collection;
import java.util.List;

public interface ReportJobRepository extends JpaRepository<ReportJob, Long> {

    List<ReportJob> findByJobStatusIn(Collection<ReportJobStatus> jobStatuses);
}
