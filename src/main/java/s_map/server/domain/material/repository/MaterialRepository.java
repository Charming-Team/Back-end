package s_map.server.domain.material.repository;

import s_map.server.domain.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    boolean existsByMaterialCode(String materialCode);
}
