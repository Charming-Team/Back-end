package s_map.server.domain.chat.service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import s_map.server.domain.chat.dto.req.EvidenceLookupFilters;
import s_map.server.domain.chat.dto.req.EvidenceLookupRequest;
import s_map.server.domain.chat.dto.res.EvidenceItemResponse;
import s_map.server.domain.material.dto.res.MaterialShortageResponse;
import s_map.server.domain.material.entity.MaterialInventory;
import s_map.server.domain.material.repository.MaterialInventoryRepository;
import s_map.server.domain.material.service.MaterialService;
import s_map.server.domain.user.entity.Role;

@Component
@RequiredArgsConstructor
public class MaterialShortageEvidenceProvider implements EvidenceProvider {

    private static final int DEFAULT_LIMIT = 5;
    private static final int MAX_LIMIT = 20;
    private static final String MATERIAL_SHORTAGE = "MATERIAL_SHORTAGE";
    private static final String MATERIAL_TARGET_TYPE = "MATERIAL";
    private static final Set<Role> MATERIAL_ALLOWED_ROLE_SET = Set.of(
            Role.OPERATOR,
            Role.EXECUTIVE,
            Role.MANUFACTURING_MANAGER
    );
    private static final List<String> MATERIAL_ALLOWED_ROLE_NAMES = List.of(
            Role.OPERATOR.name(),
            Role.EXECUTIVE.name(),
            Role.MANUFACTURING_MANAGER.name()
    );

    private final MaterialService materialService;
    private final MaterialInventoryRepository materialInventoryRepository;

    @Override
    public String intent() {
        return MATERIAL_SHORTAGE;
    }

    /**
     * 기능: 자재 부족 intent에 사용할 생산계획별 부족 자재 Evidence 목록을 만든다.
     *
     * Input:
     * - request / EvidenceLookupRequest / 사용자 Role과 조회 필터를 포함한 Evidence 요청
     *
     * Output:
     * - result / List<EvidenceItemResponse> / 부족 자재 Evidence 목록
     */
    @Override
    public List<EvidenceItemResponse> getEvidence(EvidenceLookupRequest request) {
        EvidenceLookupFilters filters = request.filters();
        if (!isRoleAllowed(request.user().role(), MATERIAL_ALLOWED_ROLE_SET)) {
            return List.of();
        }

        if (hasUnsupportedMaterialTarget(filters)) {
            return List.of();
        }

        int limit = resolveLimit(filters);
        String targetMaterialCode = resolveMaterialTargetCode(filters);
        List<MaterialShortageResponse> shortages = materialService.getMaterialShortages(limit, targetMaterialCode);
        Map<Long, MaterialInventory> inventoryMap = getInventoryMap(shortages);

        return shortages.stream()
                .map(shortage -> toMaterialShortageEvidence(
                        shortage,
                        inventoryMap.get(shortage.materialId())
                ))
                .toList();
    }

    private Map<Long, MaterialInventory> getInventoryMap(List<MaterialShortageResponse> shortages) {
        if (shortages.isEmpty()) {
            return Map.of();
        }

        List<Long> materialIds = shortages.stream()
                .map(MaterialShortageResponse::materialId)
                .distinct()
                .toList();

        return materialInventoryRepository.findByMaterialMaterialIdIn(materialIds)
                .stream()
                .collect(Collectors.toMap(
                        inventory -> inventory.getMaterial().getMaterialId(),
                        Function.identity()
                ));
    }

    private boolean isRoleAllowed(String role, Set<Role> allowedRoles) {
        if (role == null || role.isBlank()) {
            return false;
        }

        try {
            Role requestRole = Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
            return allowedRoles.contains(requestRole);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean hasUnsupportedMaterialTarget(EvidenceLookupFilters filters) {
        if (filters == null || filters.targetCode() == null || filters.targetCode().isBlank()) {
            return false;
        }

        String targetType = filters.targetType();
        return targetType != null
                && !targetType.isBlank()
                && !MATERIAL_TARGET_TYPE.equals(targetType.trim().toUpperCase(Locale.ROOT));
    }

    private String resolveMaterialTargetCode(EvidenceLookupFilters filters) {
        if (filters == null || filters.targetCode() == null || filters.targetCode().isBlank()) {
            return null;
        }
        return filters.targetCode().trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 기능: FastAPI가 전달한 limit 값을 안전한 조회 개수로 보정한다.
     *
     * Input:
     * - filters / EvidenceLookupFilters / limit 값을 포함할 수 있는 조회 필터
     *
     * Output:
     * - result / int / 1 이상 MAX_LIMIT 이하의 조회 개수
     */
    private int resolveLimit(EvidenceLookupFilters filters) {
        if (filters == null || filters.limit() == null || filters.limit() <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(filters.limit(), MAX_LIMIT);
    }

    /**
     * 기능: 부족 자재 조회 응답을 FastAPI EvidenceItem 형식으로 변환한다.
     *
     * Input:
     * - shortage / MaterialShortageResponse / 생산계획별 부족 자재 응답
     *
     * Output:
     * - result / EvidenceItemResponse / 챗봇 답변 생성에 사용할 근거 항목
     */
    private EvidenceItemResponse toMaterialShortageEvidence(
            MaterialShortageResponse shortage,
            MaterialInventory inventory
    ) {
        return new EvidenceItemResponse(
                "MATERIAL",
                buildMaterialTitle(shortage),
                buildMaterialSummary(shortage, inventory),
                "/materials/inventory/%d?mode=read".formatted(shortage.materialId()),
                "production_plan_materials",
                shortage.planMaterialId(),
                buildMaterialData(shortage, inventory),
                MATERIAL_ALLOWED_ROLE_NAMES
        );
    }

    private String buildMaterialTitle(MaterialShortageResponse shortage) {
        return "%s %s 재고 부족".formatted(
                shortage.materialCode(),
                shortage.materialName()
        );
    }

    private String buildMaterialSummary(
            MaterialShortageResponse shortage,
            MaterialInventory inventory
    ) {
        String inventorySummary = inventory != null
                ? " 현재 가용 재고는 %s%s, 안전 재고는 %s%s, 재고 상태는 %s입니다.".formatted(
                        inventory.getAvailableQuantity(),
                        shortage.unit(),
                        inventory.getSafetyStockQuantity(),
                        shortage.unit(),
                        inventory.getInventoryStatus()
                )
                : " 현재 재고 현황은 등록되어 있지 않습니다.";

        return "생산계획 %d에서 %s %s 부족 상태입니다. 필요 수량 %s%s, 예약 수량 %s%s, 부족 수량 %s%s입니다.%s"
                .formatted(
                        shortage.planId(),
                        shortage.materialCode(),
                        shortage.materialName(),
                        shortage.requiredQuantity(),
                        shortage.unit(),
                        shortage.reservedQuantity(),
                        shortage.unit(),
                        shortage.shortageQuantity(),
                        shortage.unit(),
                        inventorySummary
                );
    }

    private Map<String, Object> buildMaterialData(
            MaterialShortageResponse shortage,
            MaterialInventory inventory
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("planMaterialId", shortage.planMaterialId());
        data.put("planId", shortage.planId());
        data.put("materialId", shortage.materialId());
        data.put("materialCode", shortage.materialCode());
        data.put("materialName", shortage.materialName());
        data.put("materialType", shortage.materialType());
        data.put("unit", shortage.unit());
        data.put("requiredQuantity", shortage.requiredQuantity());
        data.put("reservedQuantity", shortage.reservedQuantity());
        data.put("consumedQuantity", shortage.consumedQuantity());
        data.put("shortageQuantity", shortage.shortageQuantity());
        data.put("materialPlanStatus", shortage.materialPlanStatus().name());
        data.put("inventoryRegistered", inventory != null);
        data.put("currentInventoryQuantity", inventory != null
                ? inventory.getCurrentQuantity()
                : BigDecimal.ZERO);
        data.put("availableInventoryQuantity", inventory != null
                ? inventory.getAvailableQuantity()
                : BigDecimal.ZERO);
        data.put("reservedInventoryQuantity", inventory != null
                ? inventory.getReservedQuantity()
                : BigDecimal.ZERO);
        data.put("safetyStockQuantity", inventory != null
                ? inventory.getSafetyStockQuantity()
                : BigDecimal.ZERO);
        data.put("expectedInboundAt", inventory != null
                ? inventory.getExpectedInboundAt()
                : null);
        data.put("expectedInboundQuantity", inventory != null
                ? inventory.getExpectedInboundQuantity()
                : null);
        data.put("inventoryStatus", inventory != null && inventory.getInventoryStatus() != null
                ? inventory.getInventoryStatus().name()
                : null);
        return data;
    }
}
