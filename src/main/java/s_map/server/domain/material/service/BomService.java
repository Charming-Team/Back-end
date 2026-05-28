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
import s_map.server.global.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BomService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

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
    public BomResponse createBom(BomCreateRequest request, AuthUser actor) {
        Material material = getMaterialEntity(request.materialId(), "BOM 등록", actor);

        validateDuplicateBom(request.productId(), request.materialId(), actor);

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
            log.warn(
                    "[BomService] BOM 등록 실패 reason=duplicate_bom productId={}, materialId={}, actorUserId={}, actorEmail={}, actorRole={}",
                    request.productId(),
                    request.materialId(),
                    actorUserId(actor),
                    actorEmail(actor),
                    actorRole(actor)
            );
            throw new CustomException(ErrorCode.DUPLICATE_BOM);
        }

        log.info(
                "[BomService] BOM 등록 성공 bomId={}, productId={}, materialId={}, materialCode={}, actorUserId={}, actorEmail={}, actorRole={}",
                savedBom.getBomId(),
                savedBom.getProductId(),
                savedBom.getMaterial().getMaterialId(),
                savedBom.getMaterial().getMaterialCode(),
                actorUserId(actor),
                actorEmail(actor),
                actorRole(actor)
        );
        return BomResponse.from(savedBom);
    }

    /**
     * 기능: BOM 목록을 페이지 단위로 조회한다.
     *
     * Input:
     * - page / int / 조회할 페이지 번호
     * - size / int / 한 페이지에 조회할 BOM 수
     *
     * Output:
     * - result / Page<BomResponse> / BOM 목록 페이지
     */
    public Page<BomResponse> getBoms(int page, int size) {
        Pageable pageable = createPageable(page, size);

        return bomRepository.findAllWithMaterial(pageable)
                .map(BomResponse::from);
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

    private Pageable createPageable(int page, int size) {
        int safePage = Math.max(page, DEFAULT_PAGE);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        return PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "bomId")
        );
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
    public BomResponse updateBom(
            Long bomId,
            BomUpdateRequest request,
            AuthUser actor
    ) {
        Bom bom = getBomEntity(bomId, actor);

        String unit = StringUtils.hasText(request.unit()) ? request.unit() : bom.getMaterial().getUnit();

        bom.update(
                request.requiredQuantityPerUnit(),
                unit,
                request.lossRate()
        );

        log.info(
                "[BomService] BOM 수정 성공 bomId={}, productId={}, materialId={}, materialCode={}, actorUserId={}, actorEmail={}, actorRole={}",
                bom.getBomId(),
                bom.getProductId(),
                bom.getMaterial().getMaterialId(),
                bom.getMaterial().getMaterialCode(),
                actorUserId(actor),
                actorEmail(actor),
                actorRole(actor)
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
    private Material getMaterialEntity(
            Long materialId,
            String action,
            AuthUser actor
    ) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> {
                    log.warn(
                            "[BomService] {} 실패 reason=material_not_found materialId={}, actorUserId={}, actorEmail={}, actorRole={}",
                            action,
                            materialId,
                            actorUserId(actor),
                            actorEmail(actor),
                            actorRole(actor)
                    );
                    return new CustomException(ErrorCode.MATERIAL_NOT_FOUND);
                });
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
    private Bom getBomEntity(Long bomId, AuthUser actor) {
        return bomRepository.findById(bomId)
                .orElseThrow(() -> {
                    log.warn(
                            "[BomService] BOM 수정 실패 reason=bom_not_found bomId={}, actorUserId={}, actorEmail={}, actorRole={}",
                            bomId,
                            actorUserId(actor),
                            actorEmail(actor),
                            actorRole(actor)
                    );
                    return new CustomException(ErrorCode.BOM_NOT_FOUND);
                });
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
    private void validateDuplicateBom(Long productId, Long materialId, AuthUser actor) {
        if (bomRepository.existsByProductIdAndMaterialMaterialId(productId, materialId)) {
            log.warn(
                    "[BomService] BOM 등록 실패 reason=duplicate_bom productId={}, materialId={}, actorUserId={}, actorEmail={}, actorRole={}",
                    productId,
                    materialId,
                    actorUserId(actor),
                    actorEmail(actor),
                    actorRole(actor)
            );
            throw new CustomException(ErrorCode.DUPLICATE_BOM);
        }
    }

    private Long actorUserId(AuthUser actor) {
        return actor != null ? actor.id() : null;
    }

    private String actorEmail(AuthUser actor) {
        return actor != null ? actor.email() : null;
    }

    private Object actorRole(AuthUser actor) {
        return actor != null ? actor.role() : null;
    }
}
