package s_map.server.domain.material.repository;

import s_map.server.domain.material.entity.Bom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BomRepository extends JpaRepository<Bom, Long> {

    List<Bom> findByProductId(Long productId);

    List<Bom> findByMaterialMaterialId(Long materialId);

    Optional<Bom> findByProductIdAndMaterialMaterialId(Long productId, Long materialId);

    boolean existsByProductIdAndMaterialMaterialId(Long productId, Long materialId);
}