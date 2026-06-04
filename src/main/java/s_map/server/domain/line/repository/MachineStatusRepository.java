package s_map.server.domain.line.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import s_map.server.domain.line.entity.MachineStatus;

import java.util.List;

public interface MachineStatusRepository extends JpaRepository<MachineStatus, Long> {

    List<MachineStatus> findByMachineIdInOrderByMachineIdAscRecordedAtDesc(List<Long> machineIds);
}
