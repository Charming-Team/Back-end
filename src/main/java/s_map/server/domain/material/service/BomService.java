package s_map.server.domain.material.service;

import s_map.server.domain.material.dto.req.BomCreateRequest;
import s_map.server.domain.material.dto.req.BomUpdateRequest;
import s_map.server.domain.material.dto.res.BomResponse;
import s_map.server.domain.material.entity.Bom;
import s_map.server.domain.material.entity.Material;
import s_map.server.domain.material.repository.BomRepository;
import s_map.server.domain.material.repository.MaterialRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BomService {

    private final BomRepository bomRepository;
    private final MaterialRepository materialRepository;

    /**
     * 기능: 제품과 자재의 BOM 정보를 등록한다.
     *
     * Input:
     * - request / BomCreateRequest / BOM 등록 요청 값
     * - request.productId / Long / 제품 고유 ID
     * - request.materialId / Long / 자재 고유 ID
     * - request.requiredQuantityPerUnit / BigDecimal / 제품 1단위 생산에 필요한 자재 소요량
     * - request.unit / String / 자재 소요량 단위
     * - request.lossRate / BigDecimal / 생산 과정에서 발생하는 손실률
     *
     * Output:
     * - result / BomResponse / 등록된 BOM 정보
     */
    @Transactional
    public BomResponse createBom(BomCreateRequest request) {
        Material material = getMaterialEntity(request.materialId());

        validateDuplicateBom(request.productId(), request.materialId());

        String unit = StringUtils.hasText(request.unit()) ? request.unit() : material.getUnit();

        Bom bom = Bom.builder()
                .productId(request.productId())
                .material(material)
                .requiredQuantityPerUnit(request.requiredQuantityPerUnit())
                .unit(unit)
                .lossRate(request.lossRate())
                .build();

        Bom savedBom;
        try {
            savedBom = bomRepository.saveAndFlush(bom);
        } catch (DataIntegrityViolationException exception) {
            throw new CustomException(ErrorCode.DUPLICATE_BOM);
        }

        return BomResponse.from(savedBom);
    }

    /**
     * 기능: 전체 BOM 목록을 조회한다.
     *
     * Input:
     * - 없음
     *
     * Output:
     * - result / List<BomResponse> / BOM 목록
     * - result[].bomId / Long / BOM 고유 ID
     * - result[].productId / Long / 제품 고유 ID
     * - result[].materialId / Long / 자재 고유 ID
     * - result[].materialCode / String / 자재 코드
     * - result[].materialName / String / 자재명
     * - result[].requiredQuantityPerUnit / BigDecimal / 제품 1단위 생산에 필요한 자재 소요량
     * - result[].lossRate / BigDecimal / 생산 과정에서 발생하는 손실률
     */
    public List<BomResponse> getBoms() {
        return bomRepository.findAllWithMaterial().stream()
                .map(BomResponse::from)
                .toList();
    }

    /**
     * 기능: 특정 제품에 필요한 BOM 목록을 조회한다.
     *
     * Input:
     * - productId / Long / BOM 목록을 조회할 제품 고유 ID
     *
     * Output:
     * - result / List<BomResponse> / 특정 제품의 BOM 목록
     */
    public List<BomResponse> getBomsByProductId(Long productId) {
        return bomRepository.findByProductIdWithMaterial(productId).stream()
                .map(BomResponse::from)
                .toList();
    }

    /**
     * 기능: 특정 BOM의 자재 소요량, 단위, 손실률을 수정한다.
     *
     * Input:
     * - bomId / Long / 수정할 BOM 고유 ID
     * - request / BomUpdateRequest / BOM 수정 요청 값
     * - request.requiredQuantityPerUnit / BigDecimal / 수정할 제품 1단위당 자재 소요량
     * - request.unit / String / 수정할 자재 소요량 단위
     * - request.lossRate / BigDecimal / 수정할 생산 손실률
     *
     * Output:
     * - result / BomResponse / 수정된 BOM 정보
     */
    @Transactional
    public BomResponse updateBom(Long bomId, BomUpdateRequest request) {
        Bom bom = getBomEntity(bomId);

        String unit = StringUtils.hasText(request.unit()) ? request.unit() : bom.getMaterial().getUnit();

        bom.update(
                request.requiredQuantityPerUnit(),
                unit,
                request.lossRate()
        );

        return BomResponse.from(bom);
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
     * 기능: BOM ID를 기준으로 BOM 엔티티를 조회하고, 존재하지 않으면 예외를 발생시킨다.
     *
     * Input:
     * - bomId / Long / 조회할 BOM 고유 ID
     *
     * Output:
     * - result / Bom / 조회된 BOM 엔티티
     */
    private Bom getBomEntity(Long bomId) {
        return bomRepository.findById(bomId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOM_NOT_FOUND));
    }

    /**
     * 기능: 같은 제품과 자재 조합의 BOM이 이미 존재하는지 검증한다.
     *
     * Input:
     * - productId / Long / 제품 고유 ID
     * - materialId / Long / 자재 고유 ID
     *
     * Output:
     * - result / void / 반환값 없음, 중복 시 예외 발생
     */
    private void validateDuplicateBom(Long productId, Long materialId) {
        if (bomRepository.existsByProductIdAndMaterialMaterialId(productId, materialId)) {
            throw new CustomException(ErrorCode.DUPLICATE_BOM);
        }
    }
}
