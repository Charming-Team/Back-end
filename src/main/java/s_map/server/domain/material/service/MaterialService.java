package s_map.server.domain.material.service;

import s_map.server.domain.material.dto.req.MaterialCreateRequest;
import s_map.server.domain.material.dto.req.MaterialUpdateRequest;
import s_map.server.domain.material.dto.res.MaterialDetailResponse;
import s_map.server.domain.material.dto.res.MaterialResponse;
import s_map.server.domain.material.dto.res.MaterialUsageResponse;
import s_map.server.domain.material.entity.Material;
import s_map.server.domain.material.entity.MaterialInventory;
import s_map.server.domain.material.entity.ProductionPlanMaterial;
import s_map.server.domain.material.repository.MaterialInventoryRepository;
import s_map.server.domain.material.repository.MaterialRepository;
import s_map.server.domain.material.repository.ProductionPlanMaterialRepository;
import s_map.server.domain.material.dto.req.MaterialInventoryUpdateRequest;
import s_map.server.domain.material.entity.InventoryStatus;

import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MaterialService {

    private final MaterialRepository materialRepository;
    private final MaterialInventoryRepository materialInventoryRepository;
    private final ProductionPlanMaterialRepository productionPlanMaterialRepository;

    /**
     * 기능: 신규 자재 기본 정보를 등록한다.
     *
     * Input:
     * - request / MaterialCreateRequest / 자재 등록 요청 값
     * - request.materialCode / String / 자재 코드, 중복 불가
     * - request.materialName / String / 자재명
     * - request.materialType / String / 자재 유형
     * - request.unit / String / 자재 단위
     * - request.description / String / 자재 설명 또는 비고
     *
     * Output:
     * - result / MaterialDetailResponse / 등록된 자재 상세 정보
     */
    @Transactional
    public MaterialDetailResponse createMaterial(MaterialCreateRequest request) {
        validateDuplicateMaterialCode(request.materialCode());

        Material material = Material.builder()
                .materialCode(request.materialCode())
                .materialName(request.materialName())
                .materialType(request.materialType())
                .unit(request.unit())
                .description(request.description())
                .build();

        Material savedMaterial = materialRepository.save(material);

        return MaterialDetailResponse.from(savedMaterial, null);
    }

    /**
     * 기능: 전체 자재 목록과 각 자재의 재고 현황을 조회한다.
     *
     * Input:
     * - 없음
     *
     * Output:
     * - result / List<MaterialResponse> / 자재 목록 및 재고 현황 목록
     * - result[].materialId / Long / 자재 고유 ID
     * - result[].materialCode / String / 자재 코드
     * - result[].materialName / String / 자재명
     * - result[].materialType / String / 자재 유형
     * - result[].unit / String / 자재 단위
     * - result[].currentQuantity / BigDecimal / 현재 보유 중인 전체 재고량
     * - result[].availableQuantity / BigDecimal / 실제 생산에 사용 가능한 재고량
     * - result[].reservedQuantity / BigDecimal / 생산계획에 이미 예약된 재고량
     * - result[].safetyStockQuantity / BigDecimal / 안전 재고 수량
     * - result[].inventoryStatus / InventoryStatus / 재고 상태
     */
    public List<MaterialResponse> getMaterials() {
        List<Material> materials = materialRepository.findAll();

        return materials.stream()
                .sorted(Comparator.comparing(Material::getMaterialId).reversed())
                .map(material -> {
                    MaterialInventory inventory = materialInventoryRepository
                            .findByMaterialMaterialId(material.getMaterialId())
                            .orElse(null);

                    return MaterialResponse.from(material, inventory);
                })
                .toList();
    }

    /**
     * 기능: 특정 자재의 기본 정보와 상세 재고 현황을 조회한다.
     *
     * Input:
     * - materialId / Long / 조회할 자재 고유 ID
     *
     * Output:
     * - result / MaterialDetailResponse / 자재 상세 정보 및 재고 현황
     * - result.materialId / Long / 자재 고유 ID
     * - result.materialCode / String / 자재 코드
     * - result.materialName / String / 자재명
     * - result.materialType / String / 자재 유형
     * - result.unit / String / 자재 단위
     * - result.description / String / 자재 설명 또는 비고
     * - result.currentQuantity / BigDecimal / 현재 보유 중인 전체 재고량
     * - result.availableQuantity / BigDecimal / 실제 생산에 사용 가능한 재고량
     * - result.reservedQuantity / BigDecimal / 생산계획에 이미 예약된 재고량
     * - result.safetyStockQuantity / BigDecimal / 안전 재고 수량
     * - result.expectedInboundAt / LocalDateTime / 입고 예정 일시
     * - result.expectedInboundQuantity / BigDecimal / 입고 예정 수량
     * - result.inventoryStatus / InventoryStatus / 재고 상태
     * - result.updatedAt / LocalDateTime / 자재 정보 수정 일시
     */
    public MaterialDetailResponse getMaterial(Long materialId) {
        Material material = getMaterialEntity(materialId);

        MaterialInventory inventory = materialInventoryRepository
                .findByMaterialMaterialId(materialId)
                .orElse(null);

        return MaterialDetailResponse.from(material, inventory);
    }

    /**
     * 기능: 특정 자재의 기본 정보를 수정한다.
     *
     * Input:
     * - materialId / Long / 수정할 자재 고유 ID
     * - request / MaterialUpdateRequest / 자재 수정 요청 값
     * - request.materialName / String / 수정할 자재명
     * - request.materialType / String / 수정할 자재 유형
     * - request.unit / String / 수정할 자재 단위
     * - request.description / String / 수정할 자재 설명 또는 비고
     *
     * Output:
     * - result / MaterialDetailResponse / 수정된 자재 상세 정보 및 재고 현황
     */
    @Transactional
    public MaterialDetailResponse updateMaterial(Long materialId, MaterialUpdateRequest request) {
        Material material = getMaterialEntity(materialId);

        material.update(
                request.materialName(),
                request.materialType(),
                request.unit(),
                request.description()
        );

        MaterialInventory inventory = materialInventoryRepository
                .findByMaterialMaterialId(materialId)
                .orElse(null);

        return MaterialDetailResponse.from(material, inventory);
    }

    /**
     * 기능: 특정 자재가 생산계획별로 얼마나 사용될 예정인지 조회한다.
     *
     * Input:
     * - materialId / Long / 사용량을 조회할 자재 고유 ID
     *
     * Output:
     * - result / MaterialUsageResponse / 자재 사용량 요약 및 생산계획별 사용량 목록
     * - result.materialId / Long / 자재 고유 ID
     * - result.materialCode / String / 자재 코드
     * - result.materialName / String / 자재명
     * - result.unit / String / 자재 단위
     * - result.currentQuantity / BigDecimal / 현재 보유 중인 전체 재고량
     * - result.availableQuantity / BigDecimal / 실제 생산에 사용 가능한 재고량
     * - result.reservedQuantity / BigDecimal / 생산계획에 이미 예약된 재고량
     * - result.safetyStockQuantity / BigDecimal / 안전 재고 수량
     * - result.totalExpectedUsage / BigDecimal / 전체 생산계획 기준 예상 필요 수량 합계
     * - result.totalReservedQuantity / BigDecimal / 전체 생산계획 기준 예약 수량 합계
     * - result.totalConsumedQuantity / BigDecimal / 전체 생산계획 기준 실제 사용 수량 합계
     * - result.totalShortageQuantity / BigDecimal / 전체 생산계획 기준 부족 수량 합계
     * - result.usages / List<MaterialUsageItemResponse> / 생산계획별 자재 사용량 상세 목록
     */
    public MaterialUsageResponse getMaterialUsage(Long materialId) {
        Material material = getMaterialEntity(materialId);

        MaterialInventory inventory = materialInventoryRepository
                .findByMaterialMaterialId(materialId)
                .orElse(null);

        List<ProductionPlanMaterial> planMaterials = productionPlanMaterialRepository
                .findByMaterialMaterialId(materialId);

        return MaterialUsageResponse.from(material, inventory, planMaterials);
    }

    /**
     * 기능: 자재 ID를 기준으로 자재 엔티티를 조회하고, 존재하지 않으면 예외를 발생시킨다.
     *
     * Input:
     * - materialId / Long / 조회할 자재 고유 ID
     *
     * Output:
     * - result / Material / 조회된 자재 엔티티
     */
    private Material getMaterialEntity(Long materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new CustomException(ErrorCode.MATERIAL_NOT_FOUND));
    }

    /**
     * 기능: 자재 코드 중복 여부를 검증하고, 이미 존재하는 코드이면 예외를 발생시킨다.
     *
     * Input:
     * - materialCode / String / 중복 검증할 자재 코드
     *
     * Output:
     * - result / void / 반환값 없음, 중복 시 예외 발생
     */
    private void validateDuplicateMaterialCode(String materialCode) {
        if (materialRepository.existsByMaterialCode(materialCode)) {
            throw new CustomException(ErrorCode.DUPLICATE_MATERIAL_CODE);
        }
    }

    /**
     * 기능: 특정 자재의 재고 현황을 등록하거나 수정한다.
     *
     * Input:
     * - materialId / Long / 재고를 등록 또는 수정할 자재 고유 ID
     * - request / MaterialInventoryUpdateRequest / 자재 재고 수정 요청 값
     * - request.currentQuantity / BigDecimal / 현재 보유 중인 전체 재고량
     * - request.reservedQuantity / BigDecimal / 생산계획에 이미 예약된 재고량
     * - request.safetyStockQuantity / BigDecimal / 안전 재고 수량
     * - request.expectedInboundAt / LocalDateTime / 입고 예정 일시
     * - request.expectedInboundQuantity / BigDecimal / 입고 예정 수량
     *
     * Output:
     * - result / MaterialDetailResponse / 수정된 자재 상세 정보 및 재고 현황
     */
    @Transactional
    public MaterialDetailResponse updateMaterialInventory(
            Long materialId,
            MaterialInventoryUpdateRequest request
    ) {
        Material material = getMaterialEntity(materialId);

        BigDecimal availableQuantity = request.currentQuantity()
                .subtract(request.reservedQuantity());

        InventoryStatus inventoryStatus = calculateInventoryStatus(
                availableQuantity,
                request.safetyStockQuantity(),
                request.expectedInboundAt(),
                request.expectedInboundQuantity()
        );

        MaterialInventory inventory = materialInventoryRepository
                .findByMaterialMaterialId(materialId)
                .map(existingInventory -> {
                    existingInventory.updateInventory(
                            request.currentQuantity(),
                            availableQuantity,
                            request.reservedQuantity(),
                            request.safetyStockQuantity(),
                            request.expectedInboundAt(),
                            request.expectedInboundQuantity(),
                            inventoryStatus
                    );

                    return existingInventory;
                })
                .orElseGet(() -> materialInventoryRepository.save(
                        MaterialInventory.builder()
                                .material(material)
                                .currentQuantity(request.currentQuantity())
                                .availableQuantity(availableQuantity)
                                .reservedQuantity(request.reservedQuantity())
                                .safetyStockQuantity(request.safetyStockQuantity())
                                .expectedInboundAt(request.expectedInboundAt())
                                .expectedInboundQuantity(request.expectedInboundQuantity())
                                .inventoryStatus(inventoryStatus)
                                .build()
                ));

        return MaterialDetailResponse.from(material, inventory);
    }

    /**
     * 기능: 가용 재고, 안전 재고, 입고 예정 정보를 기준으로 재고 상태를 계산한다.
     *
     * Input:
     * - availableQuantity / BigDecimal / 실제 생산에 사용 가능한 재고량
     * - safetyStockQuantity / BigDecimal / 안전 재고 수량
     * - expectedInboundAt / LocalDateTime / 입고 예정 일시
     * - expectedInboundQuantity / BigDecimal / 입고 예정 수량
     *
     * Output:
     * - result / InventoryStatus / 계산된 재고 상태
     */
    private InventoryStatus calculateInventoryStatus(
            BigDecimal availableQuantity,
            BigDecimal safetyStockQuantity,
            java.time.LocalDateTime expectedInboundAt,
            BigDecimal expectedInboundQuantity
    ) {
        if (availableQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return InventoryStatus.SHORTAGE;
        }

        if (availableQuantity.compareTo(safetyStockQuantity) < 0) {
            return InventoryStatus.LOW;
        }

        if (expectedInboundAt != null
                && expectedInboundQuantity != null
                && expectedInboundQuantity.compareTo(BigDecimal.ZERO) > 0) {
            return InventoryStatus.INBOUND_WAITING;
        }

        return InventoryStatus.NORMAL;
    }
}