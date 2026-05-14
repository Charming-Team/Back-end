package s_map.server.domain.material.repository;

import s_map.server.domain.material.entity.MaterialInventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialInventoryRepository extends JpaRepository<MaterialInventory, Long> {

    Optional<MaterialInventory> findByMaterialMaterialId(Long materialId);

    List<MaterialInventory> findByMaterialMaterialIdIn(List<Long> materialIds);
}
