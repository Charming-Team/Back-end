package s_map.server.domain.material.repository;

import s_map.server.domain.material.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    boolean existsByMaterialCode(String materialCode);

    Optional<Material> findByMaterialCode(String materialCode);
}