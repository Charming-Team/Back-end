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

public interface ProductionPlanMaterialRepository extends JpaRepository<ProductionPlanMaterial, Long> {

    Page<ProductionPlanMaterial> findByMaterialMaterialId(Long materialId, Pageable pageable);

    List<ProductionPlanMaterial> findByPlanId(Long planId);

    @Query("""
            select planMaterial from ProductionPlanMaterial planMaterial
            join fetch planMaterial.material
            where planMaterial.planId = :planId
            order by planMaterial.planMaterialId asc
            """)
    List<ProductionPlanMaterial> findByPlanIdWithMaterial(@Param("planId") Long planId);

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

    @Query("""
            select planMaterial from ProductionPlanMaterial planMaterial
            join fetch planMaterial.material
            where planMaterial.materialPlanStatus in :materialPlanStatuses
            order by planMaterial.planId asc, planMaterial.planMaterialId asc
            """)
    List<ProductionPlanMaterial> findByMaterialPlanStatusInWithMaterial(
            @Param("materialPlanStatuses") List<MaterialPlanStatus> materialPlanStatuses
    );

    @Query("""
            select planMaterial from ProductionPlanMaterial planMaterial
            join fetch planMaterial.material
            where planMaterial.materialPlanStatus in :materialPlanStatuses
            order by planMaterial.planId asc, planMaterial.planMaterialId asc
            """)
    List<ProductionPlanMaterial> findLimitedByMaterialPlanStatusInWithMaterial(
            @Param("materialPlanStatuses") List<MaterialPlanStatus> materialPlanStatuses,
            Pageable pageable
    );

    @Query("""
            select planMaterial from ProductionPlanMaterial planMaterial
            join fetch planMaterial.material material
            where planMaterial.materialPlanStatus in :materialPlanStatuses
              and material.materialCode = :materialCode
            order by planMaterial.planId asc, planMaterial.planMaterialId asc
            """)
    List<ProductionPlanMaterial> findLimitedByMaterialPlanStatusInAndMaterialCodeWithMaterial(
            @Param("materialPlanStatuses") List<MaterialPlanStatus> materialPlanStatuses,
            @Param("materialCode") String materialCode,
            Pageable pageable
    );
}