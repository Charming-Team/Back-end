package s_map.server.domain.line.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import s_map.server.domain.line.entity.ProductionLine;

import java.util.List;

public interface ProductionLineRepository extends JpaRepository<ProductionLine, Long> {

    List<ProductionLine> findAllByOrderByLineIdAsc();
}
