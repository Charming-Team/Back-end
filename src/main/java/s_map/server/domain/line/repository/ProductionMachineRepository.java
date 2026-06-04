package s_map.server.domain.line.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import s_map.server.domain.line.entity.ProductionMachine;

import java.util.List;

public interface ProductionMachineRepository extends JpaRepository<ProductionMachine, Long> {

    List<ProductionMachine> findByLineIdInOrderByLineIdAscMachineOrderAscMachineIdAsc(List<Long> lineIds);
}
