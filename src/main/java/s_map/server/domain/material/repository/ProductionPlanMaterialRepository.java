package s_map.server.domain.material.repository;

import s_map.server.domain.material.entity.MaterialPlanStatus;
import s_map.server.domain.material.entity.ProductionPlanMaterial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductionPlanMaterialRepository extends JpaRepository<ProductionPlanMaterial, Long> {

    List<ProductionPlanMaterial> findByProductionPlanPlanId(Long planId);

    List<ProductionPlanMaterial> findByPlanId(Long planId);

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
}