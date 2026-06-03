package s_map.server.domain.material;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import s_map.server.domain.material.entity.Bom;
import s_map.server.domain.material.entity.Material;
import s_map.server.domain.material.repository.BomRepository;
import s_map.server.domain.material.repository.MaterialInventoryRepository;
import s_map.server.domain.material.repository.MaterialRepository;
import s_map.server.domain.material.repository.ProductionPlanMaterialRepository;
import s_map.server.domain.token.repository.RefreshTokenRepository;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.entity.UserStatus;
import s_map.server.domain.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterialValidationIntegrationTest {

    private static final String PASSWORD = "Password1234!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BomRepository bomRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private MaterialInventoryRepository materialInventoryRepository;

    @Autowired
    private ProductionPlanMaterialRepository productionPlanMaterialRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        bomRepository.deleteAll();
        productionPlanMaterialRepository.deleteAll();
        materialInventoryRepository.deleteAll();
        materialRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("자재 등록은 허용되지 않은 단위를 거부한다")
    void createMaterialRejectsUnsupportedUnit() throws Exception {
        String accessToken = loginAndGetAccessToken(saveUser());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/materials")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "materialCode", "MAT-BOX-001",
                                "materialName", "박스 단위 자재",
                                "materialType", "PACKAGING",
                                "unit", "BOX",
                                "description", "허용되지 않은 단위"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-001"));
    }

    @Test
    @DisplayName("자재 수정은 허용되지 않은 단위를 거부한다")
    void updateMaterialRejectsUnsupportedUnit() throws Exception {
        Material material = saveMaterial();
        String accessToken = loginAndGetAccessToken(saveUser());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/materials/{materialId}", material.getMaterialId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "materialName", "수정 자재",
                                "materialType", "PACKAGING",
                                "unit", "METER",
                                "description", "허용되지 않은 단위"
                        ))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-001"));
    }

    @Test
    @DisplayName("BOM 등록은 허용되지 않은 단위를 거부한다")
    void createBomRejectsUnsupportedUnit() throws Exception {
        Material material = saveMaterial();
        String accessToken = loginAndGetAccessToken(saveUser());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/materials/boms")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "productId", 1L,
                                "materialId", material.getMaterialId(),
                                "requiredQuantityPerUnit", new BigDecimal("1.5000"),
                                "unit", "BOX",
                                "lossRate", new BigDecimal("0.0200")
                        ))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-001"));
    }

    @Test
    @DisplayName("BOM 수정은 허용되지 않은 단위를 거부한다")
    void updateBomRejectsUnsupportedUnit() throws Exception {
        Bom bom = bomRepository.save(Bom.builder()
                .productId(1L)
                .material(saveMaterial())
                .requiredQuantityPerUnit(new BigDecimal("1.5000"))
                .unit("KG")
                .lossRate(new BigDecimal("0.0200"))
                .build());
        String accessToken = loginAndGetAccessToken(saveUser());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/materials/boms/{bomId}", bom.getBomId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "requiredQuantityPerUnit", new BigDecimal("2.0000"),
                                "unit", "METER",
                                "lossRate", new BigDecimal("0.0300")
                        ))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-001"));
    }

    @Test
    @DisplayName("BOM 등록은 0~1 범위를 벗어난 손실률을 거부한다")
    void createBomRejectsLossRateGreaterThanOne() throws Exception {
        Material material = saveMaterial();
        String accessToken = loginAndGetAccessToken(saveUser());

        mockMvc.perform(MockMvcRequestBuilders.post("/api/materials/boms")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "productId", 1L,
                                "materialId", material.getMaterialId(),
                                "requiredQuantityPerUnit", new BigDecimal("1.5000"),
                                "unit", "KG",
                                "lossRate", new BigDecimal("2.0000")
                        ))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-001"));
    }

    @Test
    @DisplayName("BOM 수정은 0~1 범위를 벗어난 손실률을 거부한다")
    void updateBomRejectsLossRateGreaterThanOne() throws Exception {
        Bom bom = bomRepository.save(Bom.builder()
                .productId(1L)
                .material(saveMaterial())
                .requiredQuantityPerUnit(new BigDecimal("1.5000"))
                .unit("KG")
                .lossRate(new BigDecimal("0.0200"))
                .build());
        String accessToken = loginAndGetAccessToken(saveUser());

        mockMvc.perform(MockMvcRequestBuilders.put("/api/materials/boms/{bomId}", bom.getBomId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "requiredQuantityPerUnit", new BigDecimal("2.0000"),
                                "unit", "KG",
                                "lossRate", new BigDecimal("2.0000")
                        ))))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-001"));
    }

    @Test
    @DisplayName("BOM 손실률은 0.0200을 2%로 계산한다")
    void bomCalculatesRequiredQuantityWithRatioLossRate() {
        Bom bom = Bom.builder()
                .requiredQuantityPerUnit(new BigDecimal("2.0000"))
                .unit("KG")
                .lossRate(new BigDecimal("0.0200"))
                .build();

        BigDecimal requiredQuantity = bom.calculateRequiredQuantity(new BigDecimal("100.0000"));

        Assertions.assertEquals(0, requiredQuantity.compareTo(new BigDecimal("204.0000")));
    }

    private Material saveMaterial() {
        return materialRepository.save(Material.builder()
                .materialCode("MAT-ABS-BASE")
                .materialName("ABS Resin")
                .materialType("BASE_RESIN")
                .unit("KG")
                .description("ABS 원자재")
                .build());
    }

    private User saveUser() {
        return userRepository.save(User.builder()
                .name("생산 관리자")
                .email("manager@sk.com")
                .password(passwordEncoder.encode(PASSWORD))
                .role(Role.MANUFACTURING_MANAGER)
                .department("생산관리팀")
                .companyName("s_map")
                .phoneNumber("010-1234-5678")
                .status(UserStatus.ACTIVE)
                .build());
    }

    private String loginAndGetAccessToken(User user) throws Exception {
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", user.getEmail(),
                                "password", PASSWORD
                        ))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andReturn();

        Map<String, Object> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
        Object data = response.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            throw new IllegalStateException("Response data is not an object");
        }

        Object accessToken = dataMap.get("accessToken");
        if (!(accessToken instanceof String token)) {
            throw new IllegalStateException("Response data.accessToken is not a string");
        }
        return token;
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
