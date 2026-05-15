package s_map.server.domain.material.repository;

import s_map.server.domain.material.entity.Bom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BomRepository extends JpaRepository<Bom, Long> {

    @Query(
            value = "select b from Bom b join fetch b.material",
            countQuery = "select count(b) from Bom b"
    )
    Page<Bom> findAllWithMaterial(Pageable pageable);

    @Query("""
            select b from Bom b
            join fetch b.material
            where b.productId = :productId
            order by b.bomId desc
            """)
    List<Bom> findByProductIdWithMaterial(@Param("productId") Long productId);

    boolean existsByProductIdAndMaterialMaterialId(Long productId, Long materialId);
}
