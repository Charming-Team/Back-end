package charming.server;

import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import charming.server.domain.token.repository.RefreshTokenRepository;
import charming.server.domain.user.entity.Role;
import charming.server.domain.user.entity.User;
import charming.server.domain.user.entity.UserStatus;
import charming.server.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthSecurityIntegrationTest {

    private static final String PASSWORD = "password1234!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("공통 로그인")
    class Login {

        @ParameterizedTest
        @EnumSource(Role.class)
        @DisplayName("모든 role은 로그인 성공 시 access token과 refresh token을 발급받는다")
        void allRolesCanLogin(Role role) throws Exception {
            User user = saveUser(role, role.name().toLowerCase() + "@example.com", PASSWORD);

            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", user.getEmail(),
                                    "password", PASSWORD
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.data.id").value(user.getId()))
                    .andExpect(jsonPath("$.data.email").value(user.getEmail()))
                    .andExpect(jsonPath("$.data.role").value(role.name()))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(300_000))
                    .andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(86_400_000))
                    .andReturn();

            String refreshToken = dataValue(result, "refreshToken");
            assertEquals(1, refreshTokenRepository.count());
            assertTrue(refreshTokenRepository.findAll().getFirst().getTokenHash().matches("^[0-9a-f]{64}$"));
            assertNotEquals(refreshToken, refreshTokenRepository.findAll().getFirst().getTokenHash());
        }

        @Test
        @DisplayName("존재하지 않는 이메일은 401-003을 반환한다")
        void unknownEmailFails() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "missing@example.com",
                                    "password", PASSWORD
                            ))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("401-003"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("비밀번호가 틀리면 401-003을 반환하고 로그인 실패 횟수가 증가한다")
        void wrongPasswordIncrementsFailCount() throws Exception {
            User user = saveUser(Role.OPERATOR, "operator@example.com", PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", user.getEmail(),
                                    "password", "wrong-password"
                            ))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("401-003"));

            User updatedUser = userRepository.findById(user.getId()).orElseThrow();
            assertEquals(1, updatedUser.getLoginFailCount());
        }

        @Test
        @DisplayName("비활성 계정은 로그인할 수 없다")
        void inactiveUserCannotLogin() throws Exception {
            User user = saveUser(Role.OPERATOR, "inactive@example.com", PASSWORD);
            user.suspend();
            userRepository.save(user);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", user.getEmail(),
                                    "password", PASSWORD
                            ))))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("403-002"));
        }

        @Test
        @DisplayName("이메일 형식이 아니면 validation error를 반환한다")
        void invalidEmailFormatFailsValidation() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "not-email",
                                    "password", PASSWORD
                            ))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("400-001"));
        }

        @Test
        @DisplayName("로그인은 잘못된 Authorization 헤더가 있어도 JWT 필터에 막히지 않는다")
        void loginIgnoresInvalidAuthorizationHeader() throws Exception {
            User user = saveUser(Role.OPERATOR, "operator@example.com", PASSWORD);

            mockMvc.perform(post("/api/auth/login")
                            .header(HttpHeaders.AUTHORIZATION, bearer("invalid-token"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", user.getEmail(),
                                    "password", PASSWORD
                            ))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.data.email").value(user.getEmail()));
        }
    }

    @Nested
    @DisplayName("ADMIN 사용자 생성")
    class AdminUserCreate {

        @Test
        @DisplayName("ADMIN 토큰으로 OPERATOR 사용자를 생성할 수 있다")
        void adminCanCreateOperator() throws Exception {
            String adminAccessToken = loginAndGetAccessToken(saveUser(Role.ADMIN, "admin@example.com", PASSWORD));

            mockMvc.perform(post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("operator01@example.com", "operator1234!", "operator1234!", Role.OPERATOR)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.data.name").value("김길동"))
                    .andExpect(jsonPath("$.data.email").value("operator01@example.com"))
                    .andExpect(jsonPath("$.data.role").value("OPERATOR"))
                    .andExpect(jsonPath("$.data.department").value("생산관리팀"))
                    .andExpect(jsonPath("$.data.companyName").value("Charming"))
                    .andExpect(jsonPath("$.data.phoneNumber").value("010-1234-5678"))
                    .andExpect(jsonPath("$.data.password").doesNotExist());

            User createdUser = userRepository.findByEmail("operator01@example.com").orElseThrow();
            assertTrue(passwordEncoder.matches("operator1234!", createdUser.getPassword()));
            assertNotEquals("operator1234!", createdUser.getPassword());
        }

        @Test
        @DisplayName("토큰이 없으면 사용자 생성은 401을 반환한다")
        void createUserWithoutTokenFails() throws Exception {
            mockMvc.perform(post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("operator01@example.com", "operator1234!", "operator1234!", Role.OPERATOR)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("401"));
        }

        @Test
        @DisplayName("ADMIN이 아닌 role의 토큰으로는 사용자 생성이 403을 반환한다")
        void nonAdminCannotCreateUser() throws Exception {
            String operatorAccessToken = loginAndGetAccessToken(saveUser(Role.OPERATOR, "operator@example.com", PASSWORD));

            mockMvc.perform(post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(operatorAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("new-operator@example.com", "operator1234!", "operator1234!", Role.OPERATOR)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("403"));
        }

        @Test
        @DisplayName("비밀번호 확인이 다르면 400-003을 반환한다")
        void passwordConfirmMismatchFails() throws Exception {
            String adminAccessToken = loginAndGetAccessToken(saveUser(Role.ADMIN, "admin@example.com", PASSWORD));

            mockMvc.perform(post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("operator01@example.com", "operator1234!", "different1234!", Role.OPERATOR)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("400-003"));
        }

        @Test
        @DisplayName("중복 이메일이면 409-001을 반환한다")
        void duplicateEmailFails() throws Exception {
            String adminAccessToken = loginAndGetAccessToken(saveUser(Role.ADMIN, "admin@example.com", PASSWORD));
            saveUser(Role.OPERATOR, "operator01@example.com", PASSWORD);

            mockMvc.perform(post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("operator01@example.com", "operator1234!", "operator1234!", Role.OPERATOR)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("409-001"));
        }

        @Test
        @DisplayName("잘못된 role 값이면 400을 반환한다")
        void invalidRoleFails() throws Exception {
            String adminAccessToken = loginAndGetAccessToken(saveUser(Role.ADMIN, "admin@example.com", PASSWORD));

            mockMvc.perform(post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "name": "김길동",
                                      "email": "operator01@example.com",
                                      "password": "operator1234!",
                                      "passwordConfirm": "operator1234!",
                                      "role": "INVALID_ROLE",
                                      "department": "생산관리팀",
                                      "companyName": "Charming",
                                      "phoneNumber": "010-1234-5678"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("400"));
        }
    }

    @Nested
    @DisplayName("JWT 필터와 토큰 재발급")
    class Token {

        @Test
        @DisplayName("Refresh Token 재발급은 새 토큰을 발급하고 기존 Refresh Token을 폐기한다")
        void refreshTokenRotates() throws Exception {
            User user = saveUser(Role.EXECUTIVE, "executive@example.com", PASSWORD);
            MvcResult loginResult = login(user);
            String oldAccessToken = dataValue(loginResult, "accessToken");
            String oldRefreshToken = dataValue(loginResult, "refreshToken");

            MvcResult refreshResult = mockMvc.perform(post("/api/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", oldRefreshToken))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andReturn();

            String newAccessToken = dataValue(refreshResult, "accessToken");
            String newRefreshToken = dataValue(refreshResult, "refreshToken");
            assertNotEquals(oldAccessToken, newAccessToken);
            assertNotEquals(oldRefreshToken, newRefreshToken);
            assertEquals(2, refreshTokenRepository.count());
            assertEquals(1, refreshTokenRepository.findAll().stream().filter(token -> !token.isRevoked()).count());

            mockMvc.perform(post("/api/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", oldRefreshToken))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("401-001"));
        }

        @Test
        @DisplayName("Access Token으로 토큰 재발급을 요청하면 401-001을 반환한다")
        void accessTokenCannotBeUsedAsRefreshToken() throws Exception {
            User user = saveUser(Role.OPERATOR, "operator@example.com", PASSWORD);
            String accessToken = dataValue(login(user), "accessToken");

            mockMvc.perform(post("/api/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", accessToken))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("401-001"));
        }

        @Test
        @DisplayName("Refresh Token 재발급은 잘못된 Authorization 헤더가 있어도 body의 Refresh Token으로 처리한다")
        void refreshTokenIgnoresInvalidAuthorizationHeader() throws Exception {
            User user = saveUser(Role.OPERATOR, "operator@example.com", PASSWORD);
            String refreshToken = dataValue(login(user), "refreshToken");

            mockMvc.perform(post("/api/token/refresh")
                            .header(HttpHeaders.AUTHORIZATION, bearer("invalid-token"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", refreshToken))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON200"))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("위조된 Access Token은 보호 API에서 401-001을 반환한다")
        void tamperedAccessTokenFails() throws Exception {
            User admin = saveUser(Role.ADMIN, "admin@example.com", PASSWORD);
            String accessToken = loginAndGetAccessToken(admin);
            String tamperedToken = accessToken.substring(0, accessToken.length() - 2) + "xx";

            mockMvc.perform(post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tamperedToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("operator01@example.com", "operator1234!", "operator1234!", Role.OPERATOR)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("401-001"));
        }

        @Test
        @DisplayName("기존 Access Token은 계정 상태 변경 후에도 만료 전까지 클레임 기반으로 동작한다")
        void existingAccessTokenUsesClaimsUntilItExpires() throws Exception {
            User admin = saveUser(Role.ADMIN, "admin@example.com", PASSWORD);
            String accessToken = loginAndGetAccessToken(admin);
            admin.suspend();
            userRepository.save(admin);

            mockMvc.perform(post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("operator01@example.com", "operator1234!", "operator1234!", Role.OPERATOR)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value("COMMON200"));
        }
    }

    private User saveUser(Role role, String email, String password) {
        User user = User.builder()
                .name(role.name() + " 사용자")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .department("생산관리팀")
                .companyName("Charming")
                .phoneNumber("010-0000-0000")
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    private MvcResult login(User user) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", user.getEmail(),
                                "password", PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String loginAndGetAccessToken(User user) throws Exception {
        return dataValue(login(user), "accessToken");
    }

    private String createUserJson(String email, String password, String passwordConfirm, Role role) throws Exception {
        return json(Map.of(
                "name", "김길동",
                "email", email,
                "password", password,
                "passwordConfirm", passwordConfirm,
                "role", role.name(),
                "department", "생산관리팀",
                "companyName", "Charming",
                "phoneNumber", "010-1234-5678"
        ));
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String dataValue(MvcResult result, String fieldName) throws Exception {
        Map<String, Object> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return (String) data.get(fieldName);
    }
}
