package s_map.server.domain.material.repository;

import s_map.server.domain.material.dto.res.MaterialUsageTotals;
import s_map.server.domain.material.entity.MaterialPlanStatus;
import s_map.server.domain.material.entity.ProductionPlanMaterial;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductionPlanMaterialRepository extends JpaRepository<ProductionPlanMaterial, Long> {

    List<ProductionPlanMaterial> findByPlanId(Long planId);

    Page<ProductionPlanMaterial> findByMaterialMaterialId(Long materialId, Pageable pageable);

    @Query("""
            select new s_map.server.domain.material.dto.res.MaterialUsageTotals(
                sum(planMaterial.requiredQuantity),
                sum(planMaterial.reservedQuantity),
                sum(planMaterial.consumedQuantity),
                sum(planMaterial.shortageQuantity)
            )
            from ProductionPlanMaterial planMaterial
            where planMaterial.material.materialId = :materialId
            """)
    MaterialUsageTotals sumUsageByMaterialId(@Param("materialId") Long materialId);

    Optional<ProductionPlanMaterial> findByPlanIdAndMaterialMaterialId(
            Long planId,
            Long materialId
    );

    boolean existsByPlanIdAndMaterialMaterialId(
            Long planId,
            Long materialId
    );

    List<ProductionPlanMaterial> findByMaterialPlanStatus(MaterialPlanStatus materialPlanStatus);

    List<ProductionPlanMaterial> findByMaterialPlanStatusIn(List<MaterialPlanStatus> materialPlanStatuses);

    @Query("""
            select planMaterial from ProductionPlanMaterial planMaterial
            join fetch planMaterial.material
            where planMaterial.materialPlanStatus in :materialPlanStatuses
            order by planMaterial.planId asc, planMaterial.planMaterialId asc
            """)
    List<ProductionPlanMaterial> findByMaterialPlanStatusInWithMaterial(
            @Param("materialPlanStatuses") List<MaterialPlanStatus> materialPlanStatuses
    );
}
