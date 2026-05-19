package s_map.server.domain.chat.service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.chat.dto.req.EvidenceLookupFilters;
import s_map.server.domain.chat.dto.req.EvidenceLookupRequest;
import s_map.server.domain.chat.dto.res.EvidenceItemResponse;
import s_map.server.domain.chat.dto.res.EvidenceLookupResponse;
import s_map.server.domain.material.dto.res.MaterialShortageResponse;
import s_map.server.domain.material.service.MaterialService;
import s_map.server.domain.user.entity.Role;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatEvidenceService {

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

    /**
     * 기능: FastAPI 챗봇이 요청한 질문 의도에 맞는 RDB Evidence를 조회한다.
     *
     * Input:
     * - request / EvidenceLookupRequest / 챗봇 Evidence 조회 요청 값
     * - request.intent / String / 질문 의도. 예: MATERIAL_SHORTAGE
     * - request.question / String / 사용자 원문 질문
     * - request.user / EvidenceLookupUser / 사용자 ID, Role, 회사명 메타데이터
     * - request.filters / EvidenceLookupFilters / 질문에서 추출한 조회 힌트
     *
     * Output:
     * - result / EvidenceLookupResponse / FastAPI EvidenceResult와 호환되는 RDB 근거 응답
     * - result.intent / String / 조회에 사용한 질문 의도
     * - result.basisTime / OffsetDateTime / RDB 근거 조회 기준 시각
     * - result.items / List<EvidenceItemResponse> / 답변 생성에 사용할 근거 목록
     */
    public EvidenceLookupResponse lookup(EvidenceLookupRequest request) {
        String intent = request.intent().trim().toUpperCase(Locale.ROOT);
        List<EvidenceItemResponse> items = switch (intent) {
            case MATERIAL_SHORTAGE -> buildMaterialShortageItems(
                    request.user().role(),
                    request.filters()
            );
            default -> List.of();
        };

        return new EvidenceLookupResponse(intent, OffsetDateTime.now(), items);
    }

    /**
     * 기능: 자재 부족 intent에 사용할 생산계획별 부족 자재 Evidence 목록을 만든다.
     *
     * Input:
     * - role / String / 요청 사용자 Role
     * - filters / EvidenceLookupFilters / 조회 개수와 대상 힌트
     *
     * Output:
     * - result / List<EvidenceItemResponse> / 부족 자재 Evidence 목록
     */
    private List<EvidenceItemResponse> buildMaterialShortageItems(
            String role,
            EvidenceLookupFilters filters
    ) {
        if (!isRoleAllowed(role, MATERIAL_ALLOWED_ROLE_SET)) {
            return List.of();
        }

        if (hasUnsupportedMaterialTarget(filters)) {
            return List.of();
        }

        int limit = resolveLimit(filters);
        String targetMaterialCode = resolveMaterialTargetCode(filters);
        return materialService.getMaterialShortages(limit, targetMaterialCode).stream()
                .map(this::toMaterialShortageEvidence)
                .toList();
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
    private EvidenceItemResponse toMaterialShortageEvidence(MaterialShortageResponse shortage) {
        return new EvidenceItemResponse(
                "MATERIAL",
                buildMaterialTitle(shortage),
                buildMaterialSummary(shortage),
                "/materials/inventory/%d?mode=read".formatted(shortage.materialId()),
                "production_plan_materials",
                shortage.planMaterialId(),
                buildMaterialData(shortage),
                MATERIAL_ALLOWED_ROLE_NAMES
        );
    }

    private String buildMaterialTitle(MaterialShortageResponse shortage) {
        return "%s %s 재고 부족".formatted(
                shortage.materialCode(),
                shortage.materialName()
        );
    }

    private String buildMaterialSummary(MaterialShortageResponse shortage) {
        return "생산계획 %d에서 %s %s 부족 상태입니다. 필요 수량 %s%s, 예약 수량 %s%s, 부족 수량 %s%s입니다."
                .formatted(
                        shortage.planId(),
                        shortage.materialCode(),
                        shortage.materialName(),
                        shortage.requiredQuantity(),
                        shortage.unit(),
                        shortage.reservedQuantity(),
                        shortage.unit(),
                        shortage.shortageQuantity(),
                        shortage.unit()
                );
    }

    private Map<String, Object> buildMaterialData(MaterialShortageResponse shortage) {
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
        return data;
    }
}
