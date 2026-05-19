package s_map.server.domain.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import s_map.server.domain.material.entity.Material;
import s_map.server.domain.material.entity.MaterialPlanStatus;
import s_map.server.domain.material.entity.ProductionPlanMaterial;
import s_map.server.domain.material.repository.MaterialRepository;
import s_map.server.domain.material.repository.ProductionPlanMaterialRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.internal.chat.evidence-token=test-internal-token")
class ChatEvidenceIntegrationTest {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private ProductionPlanMaterialRepository productionPlanMaterialRepository;

    @BeforeEach
    void setUp() {
        productionPlanMaterialRepository.deleteAll();
        materialRepository.deleteAll();
    }

    @Test
    @DisplayName("내부 토큰이 유효하면 자재 부족 Evidence를 반환한다")
    void lookupEvidenceReturnsMaterialShortageEvidence() throws Exception {
        Material material = materialRepository.save(Material.builder()
                .materialCode("RM-AL-001")
                .materialName("알루미늄 원자재")
                .materialType("원자재")
                .unit("KG")
                .build());
        ProductionPlanMaterial planMaterial = productionPlanMaterialRepository.save(ProductionPlanMaterial.builder()
                .planId(1001L)
                .material(material)
                .requiredQuantity(new BigDecimal("150.0000"))
                .reservedQuantity(new BigDecimal("90.0000"))
                .consumedQuantity(BigDecimal.ZERO)
                .shortageQuantity(new BigDecimal("60.0000"))
                .materialPlanStatus(MaterialPlanStatus.SHORTAGE)
                .build());

        mockMvc.perform(MockMvcRequestBuilders.post("/internal/chat/evidence")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sessionId", 10,
                                "messageId", 24,
                                "intent", "MATERIAL_SHORTAGE",
                                "question", "자재 부족으로 영향받는 생산계획 알려줘",
                                "user", Map.of(
                                        "userId", 1,
                                        "role", "MANUFACTURING_MANAGER",
                                        "companyName", "S-MAP"
                                ),
                                "filters", Map.of("limit", 5)
                        ))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.intent").value("MATERIAL_SHORTAGE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.basisTime").isString())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.basisTime").isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].type").value("MATERIAL"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].title").value("RM-AL-001 알루미늄 원자재 재고 부족"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].url").value("/materials/inventory/" + material.getMaterialId() + "?mode=read"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].source").value("production_plan_materials"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].referenceId").value(planMaterial.getPlanMaterialId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].data.planId").value(1001))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].data.materialCode").value("RM-AL-001"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].data.materialPlanStatus").value("SHORTAGE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].allowedRoles[0]").value("OPERATOR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].allowedRoles[1]").value("EXECUTIVE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].allowedRoles[2]").value("MANUFACTURING_MANAGER"));
    }

    @Test
    @DisplayName("서비스 관리자 Role은 자재 부족 Evidence를 조회하지 않는다")
    void lookupEvidenceReturnsEmptyItemsForAdminRole() throws Exception {
        saveShortageMaterial("RM-AL-001", "알루미늄 원자재", 1001L);

        mockMvc.perform(MockMvcRequestBuilders.post("/internal/chat/evidence")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sessionId", 10,
                                "messageId", 24,
                                "intent", "MATERIAL_SHORTAGE",
                                "question", "자재 부족으로 영향받는 생산계획 알려줘",
                                "user", Map.of(
                                        "userId", 1,
                                        "role", "ADMIN",
                                        "companyName", "S-MAP"
                                ),
                                "filters", Map.of("limit", 5)
                        ))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.intent").value("MATERIAL_SHORTAGE"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items").isEmpty());
    }

    @Test
    @DisplayName("limit은 자재 부족 Evidence 조회 개수를 제한한다")
    void lookupEvidenceAppliesLimit() throws Exception {
        saveShortageMaterial("RM-AL-001", "알루미늄 원자재", 1001L);
        saveShortageMaterial("RM-CU-002", "구리 원자재", 1002L);

        mockMvc.perform(MockMvcRequestBuilders.post("/internal/chat/evidence")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sessionId", 10,
                                "messageId", 24,
                                "intent", "MATERIAL_SHORTAGE",
                                "question", "자재 부족 한 건만 알려줘",
                                "user", Map.of(
                                        "userId", 1,
                                        "role", "MANUFACTURING_MANAGER",
                                        "companyName", "S-MAP"
                                ),
                                "filters", Map.of("limit", 1)
                        ))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].data.materialCode").value("RM-AL-001"));
    }

    @Test
    @DisplayName("자재 targetCode가 있으면 해당 자재 Evidence만 반환한다")
    void lookupEvidenceFiltersByMaterialTargetCode() throws Exception {
        saveShortageMaterial("RM-AL-001", "알루미늄 원자재", 1001L);
        saveShortageMaterial("RM-CU-002", "구리 원자재", 1002L);

        mockMvc.perform(MockMvcRequestBuilders.post("/internal/chat/evidence")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sessionId", 10,
                                "messageId", 24,
                                "intent", "MATERIAL_SHORTAGE",
                                "question", "RM-CU-002 자재 부족 현황 알려줘",
                                "user", Map.of(
                                        "userId", 1,
                                        "role", "MANUFACTURING_MANAGER",
                                        "companyName", "S-MAP"
                                ),
                                "filters", Map.of(
                                        "limit", 5,
                                        "targetType", "MATERIAL",
                                        "targetCode", "rm-cu-002"
                                )
                        ))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items[0].data.materialCode").value("RM-CU-002"));
    }

    @Test
    @DisplayName("자재 부족 intent에서 지원하지 않는 targetType이면 Evidence를 반환하지 않는다")
    void lookupEvidenceReturnsEmptyItemsForUnsupportedTargetType() throws Exception {
        saveShortageMaterial("RM-AL-001", "알루미늄 원자재", 1001L);

        mockMvc.perform(MockMvcRequestBuilders.post("/internal/chat/evidence")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sessionId", 10,
                                "messageId", 24,
                                "intent", "MATERIAL_SHORTAGE",
                                "question", "LINE-A01 자재 부족 현황 알려줘",
                                "user", Map.of(
                                        "userId", 1,
                                        "role", "MANUFACTURING_MANAGER",
                                        "companyName", "S-MAP"
                                ),
                                "filters", Map.of(
                                        "limit", 5,
                                        "targetType", "LINE",
                                        "targetCode", "LINE-A01"
                                )
                        ))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items").isEmpty());
    }

    @Test
    @DisplayName("내부 토큰이 없으면 401-004를 반환한다")
    void lookupEvidenceWithoutInternalTokenFails() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/internal/chat/evidence")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sessionId", 10,
                                "messageId", 24,
                                "intent", "MATERIAL_SHORTAGE",
                                "question", "자재 부족으로 영향받는 생산계획 알려줘",
                                "user", Map.of(
                                        "userId", 1,
                                        "role", "MANUFACTURING_MANAGER",
                                        "companyName", "S-MAP"
                                ),
                                "filters", Map.of("limit", 5)
                        ))))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("401-004"));
    }

    @Test
    @DisplayName("아직 구현되지 않은 intent는 빈 Evidence를 반환한다")
    void lookupEvidenceReturnsEmptyItemsForUnsupportedIntent() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/internal/chat/evidence")
                        .header(INTERNAL_TOKEN_HEADER, "test-internal-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "sessionId", 10,
                                "messageId", 24,
                                "intent", "PRODUCTION_PLAN",
                                "question", "다음 주 생산계획 변경 일정 보여줘",
                                "user", Map.of(
                                        "userId", 1,
                                        "role", "MANUFACTURING_MANAGER",
                                        "companyName", "S-MAP"
                                ),
                                "filters", Map.of("limit", 5)
                        ))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.intent").value("PRODUCTION_PLAN"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items").isArray())
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.items").isEmpty());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private ProductionPlanMaterial saveShortageMaterial(
            String materialCode,
            String materialName,
            Long planId
    ) {
        Material material = materialRepository.save(Material.builder()
                .materialCode(materialCode)
                .materialName(materialName)
                .materialType("원자재")
                .unit("KG")
                .build());

        return productionPlanMaterialRepository.save(ProductionPlanMaterial.builder()
                .planId(planId)
                .material(material)
                .requiredQuantity(new BigDecimal("150.0000"))
                .reservedQuantity(new BigDecimal("90.0000"))
                .consumedQuantity(BigDecimal.ZERO)
                .shortageQuantity(new BigDecimal("60.0000"))
                .materialPlanStatus(MaterialPlanStatus.SHORTAGE)
                .build());
    }
}
