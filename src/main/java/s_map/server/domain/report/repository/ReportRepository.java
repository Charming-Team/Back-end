package s_map.server.domain.report.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import s_map.server.domain.report.entity.Report;
import s_map.server.domain.report.entity.ReportType;

import java.util.Collection;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Report> findAllByReportTypeInOrderByCreatedAtDesc(
            Collection<ReportType> reportTypes,
            Pageable pageable
    );
}
