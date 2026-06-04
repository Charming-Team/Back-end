package s_map.server.domain.line.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import s_map.server.domain.line.entity.MachineStatus;

import java.util.List;

public interface MachineStatusRepository extends JpaRepository<MachineStatus, Long> {

    @Query(
            value = """
                    SELECT latest_machine_statuses.*
                    FROM (
                        SELECT DISTINCT ON (machine_statuses.machine_id)
                            machine_statuses.*
                        FROM machine_statuses
                        WHERE machine_statuses.machine_id IN (:machineIds)
                        ORDER BY machine_statuses.machine_id ASC, machine_statuses.recorded_at DESC
                    ) latest_machine_statuses
                    ORDER BY latest_machine_statuses.machine_id ASC
                    """,
            nativeQuery = true
    )
    List<MachineStatus> findLatestByMachineIdIn(@Param("machineIds") List<Long> machineIds);
}
