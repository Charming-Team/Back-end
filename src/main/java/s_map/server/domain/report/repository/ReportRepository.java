package s_map.server.domain.report.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import s_map.server.domain.report.entity.Report;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findAllByOrderByCreatedAtDesc();
}