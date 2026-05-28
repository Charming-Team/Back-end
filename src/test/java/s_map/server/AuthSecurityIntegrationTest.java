package s_map.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
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
import s_map.server.domain.token.repository.RefreshTokenRepository;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.entity.UserStatus;
import s_map.server.domain.user.repository.UserRepository;

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

            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", user.getEmail(),
                                    "password", PASSWORD
                            ))))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON200"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(user.getId()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(user.getEmail()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.role").value(role.name()))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessTokenExpiresIn").value(300_000))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.refreshTokenExpiresIn").value(86_400_000))
                    .andReturn();

            String refreshToken = dataValue(result, "refreshToken");
            Assertions.assertEquals(1, refreshTokenRepository.count());
            Assertions.assertTrue(refreshTokenRepository.findAll().getFirst().getTokenHash().matches("^[0-9a-f]{64}$"));
            Assertions.assertNotEquals(refreshToken, refreshTokenRepository.findAll().getFirst().getTokenHash());
        }

        @Test
        @DisplayName("존재하지 않는 이메일은 401-003을 반환한다")
        void unknownEmailFails() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "missing@example.com",
                                    "password", PASSWORD
                            ))))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("401-003"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data").doesNotExist());
        }

        @Test
        @DisplayName("비밀번호가 틀리면 401-003을 반환한다")
        void wrongPasswordFails() throws Exception {
            User user = saveUser(Role.OPERATOR, "operator@example.com", PASSWORD);

            mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", user.getEmail(),
                                    "password", "wrong-password"
                            ))))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("401-003"));
        }

        @Test
        @DisplayName("비활성 계정은 로그인할 수 없다")
        void inactiveUserCannotLogin() throws Exception {
            User user = saveUser(Role.OPERATOR, "inactive@example.com", PASSWORD);
            user.suspend();
            userRepository.save(user);

            mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", user.getEmail(),
                                    "password", PASSWORD
                            ))))
                    .andExpect(MockMvcResultMatchers.status().isForbidden())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("403-002"));
        }

        @Test
        @DisplayName("이메일 형식이 아니면 validation error를 반환한다")
        void invalidEmailFormatFailsValidation() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", "not-email",
                                    "password", PASSWORD
                            ))))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-001"));
        }

        @Test
        @DisplayName("로그인은 잘못된 Authorization 헤더가 있어도 JWT 필터에 막히지 않는다")
        void loginIgnoresInvalidAuthorizationHeader() throws Exception {
            User user = saveUser(Role.OPERATOR, "operator@example.com", PASSWORD);

            mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                            .header(HttpHeaders.AUTHORIZATION, bearer("invalid-token"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "email", user.getEmail(),
                                    "password", PASSWORD
                            ))))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON200"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value(user.getEmail()));
        }
    }

    @Nested
    @DisplayName("ADMIN 사용자 생성")
    class AdminUserCreate {

        @Test
        @DisplayName("ADMIN 토큰으로 OPERATOR 사용자를 생성할 수 있다")
        void adminCanCreateOperator() throws Exception {
            String adminAccessToken = loginAndGetAccessToken(saveUser(Role.ADMIN, "admin@example.com", PASSWORD));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("operator01@example.com", "operator1234!", "operator1234!", Role.OPERATOR)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON200"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.name").value("김길동"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value("operator01@example.com"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.role").value("OPERATOR"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.department").value("생산관리팀"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.companyName").value("s_map"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.phoneNumber").value("010-1234-5678"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.password").doesNotExist());

            User createdUser = userRepository.findByEmail("operator01@example.com").orElseThrow();
            Assertions.assertTrue(passwordEncoder.matches("operator1234!", createdUser.getPassword()));
            Assertions.assertNotEquals("operator1234!", createdUser.getPassword());
        }

        @Test
        @DisplayName("토큰이 없으면 사용자 생성은 401을 반환한다")
        void createUserWithoutTokenFails() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("operator01@example.com", "operator1234!", "operator1234!", Role.OPERATOR)))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("401"));
        }

        @Test
        @DisplayName("ADMIN이 아닌 role의 토큰으로는 사용자 생성이 403을 반환한다")
        void nonAdminCannotCreateUser() throws Exception {
            String operatorAccessToken = loginAndGetAccessToken(saveUser(Role.OPERATOR, "operator@example.com", PASSWORD));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(operatorAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("new-operator@example.com", "operator1234!", "operator1234!", Role.OPERATOR)))
                    .andExpect(MockMvcResultMatchers.status().isForbidden())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("403"));
        }

        @Test
        @DisplayName("비밀번호 확인이 다르면 400-003을 반환한다")
        void passwordConfirmMismatchFails() throws Exception {
            String adminAccessToken = loginAndGetAccessToken(saveUser(Role.ADMIN, "admin@example.com", PASSWORD));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("operator01@example.com", "operator1234!", "different1234!", Role.OPERATOR)))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-003"));
        }

        @Test
        @DisplayName("중복 이메일이면 409-001을 반환한다")
        void duplicateEmailFails() throws Exception {
            String adminAccessToken = loginAndGetAccessToken(saveUser(Role.ADMIN, "admin@example.com", PASSWORD));
            saveUser(Role.OPERATOR, "operator01@example.com", PASSWORD);

            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("operator01@example.com", "operator1234!", "operator1234!", Role.OPERATOR)))
                    .andExpect(MockMvcResultMatchers.status().isConflict())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("409-001"));
        }

        @Test
        @DisplayName("잘못된 role 값이면 400을 반환한다")
        void invalidRoleFails() throws Exception {
            String adminAccessToken = loginAndGetAccessToken(saveUser(Role.ADMIN, "admin@example.com", PASSWORD));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/users")
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
                                      "companyName": "s_map",
                                      "phoneNumber": "010-1234-5678"
                                    }
                                    """))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400"));
        }
    }

    @Nested
    @DisplayName("ADMIN 사용자 삭제")
    class AdminUserDelete {

        @Test
        @DisplayName("ADMIN 토큰으로 일반 사용자를 삭제 처리할 수 있다")
        void adminCanDeleteOperator() throws Exception {
            User admin = saveUser(Role.ADMIN, "admin@example.com", PASSWORD);
            User operator = saveUser(Role.OPERATOR, "operator@example.com", PASSWORD);
            String adminAccessToken = loginAndGetAccessToken(admin);

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/users/{userId}", operator.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON200"));

            User deletedUser = userRepository.findById(operator.getId()).orElseThrow();
            Assertions.assertEquals(UserStatus.WITHDRAWN, deletedUser.getStatus());
        }

        @Test
        @DisplayName("삭제할 사용자가 없으면 404-201을 반환한다")
        void missingUserCannotBeDeleted() throws Exception {
            User admin = saveUser(Role.ADMIN, "admin@example.com", PASSWORD);
            String adminAccessToken = loginAndGetAccessToken(admin);

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/users/{userId}", 999_999L)
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken)))
                    .andExpect(MockMvcResultMatchers.status().isNotFound())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("404-201"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("삭제할 수 없는 사용자 정보입니다."));
        }

        @Test
        @DisplayName("이미 삭제된 사용자는 409-201을 반환한다")
        void alreadyDeletedUserCannotBeDeletedAgain() throws Exception {
            User admin = saveUser(Role.ADMIN, "admin@example.com", PASSWORD);
            User operator = saveUser(Role.OPERATOR, "operator@example.com", PASSWORD);
            operator.withdraw();
            userRepository.save(operator);
            String adminAccessToken = loginAndGetAccessToken(admin);

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/users/{userId}", operator.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken)))
                    .andExpect(MockMvcResultMatchers.status().isConflict())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("409-201"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("이미 삭제된 사용자입니다."));
        }

        @Test
        @DisplayName("시스템 관리자는 본인 계정을 삭제할 수 없다")
        void adminCannotDeleteSelf() throws Exception {
            User admin = saveUser(Role.ADMIN, "admin@example.com", PASSWORD);
            String adminAccessToken = loginAndGetAccessToken(admin);

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/users/{userId}", admin.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken)))
                    .andExpect(MockMvcResultMatchers.status().isForbidden())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("403-202"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("본인 계정은 삭제할 수 없습니다."));
        }

        @Test
        @DisplayName("시스템 관리자 계정은 삭제할 수 없다")
        void adminCannotDeleteAnotherAdmin() throws Exception {
            User admin = saveUser(Role.ADMIN, "admin@example.com", PASSWORD);
            User anotherAdmin = saveUser(Role.ADMIN, "another-admin@example.com", PASSWORD);
            String adminAccessToken = loginAndGetAccessToken(admin);

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/users/{userId}", anotherAdmin.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(adminAccessToken)))
                    .andExpect(MockMvcResultMatchers.status().isForbidden())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("403-203"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("시스템 관리자 계정은 삭제할 수 없습니다."));
        }

        @Test
        @DisplayName("삭제 권한이 없는 사용자는 403-201을 반환한다")
        void nonAdminCannotDeleteUser() throws Exception {
            User operator = saveUser(Role.OPERATOR, "operator@example.com", PASSWORD);
            User executive = saveUser(Role.EXECUTIVE, "executive@example.com", PASSWORD);
            String operatorAccessToken = loginAndGetAccessToken(operator);

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/admin/users/{userId}", executive.getId())
                            .header(HttpHeaders.AUTHORIZATION, bearer(operatorAccessToken)))
                    .andExpect(MockMvcResultMatchers.status().isForbidden())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("403-201"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("사용자 삭제 권한이 없습니다."));
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

            MvcResult refreshResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", oldRefreshToken))))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.refreshToken").isNotEmpty())
                    .andReturn();

            String newAccessToken = dataValue(refreshResult, "accessToken");
            String newRefreshToken = dataValue(refreshResult, "refreshToken");
            Assertions.assertNotEquals(oldAccessToken, newAccessToken);
            Assertions.assertNotEquals(oldRefreshToken, newRefreshToken);
            Assertions.assertEquals(2, refreshTokenRepository.count());
            Assertions.assertEquals(1, refreshTokenRepository.findAll().stream().filter(token -> !token.isRevoked()).count());

            mockMvc.perform(MockMvcRequestBuilders.post("/api/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", oldRefreshToken))))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("401-001"));
        }

        @Test
        @DisplayName("Access Token으로 토큰 재발급을 요청하면 401-001을 반환한다")
        void accessTokenCannotBeUsedAsRefreshToken() throws Exception {
            User user = saveUser(Role.OPERATOR, "operator@example.com", PASSWORD);
            String accessToken = dataValue(login(user), "accessToken");

            mockMvc.perform(MockMvcRequestBuilders.post("/api/token/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", accessToken))))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("401-001"));
        }

        @Test
        @DisplayName("Refresh Token 재발급은 잘못된 Authorization 헤더가 있어도 body의 Refresh Token으로 처리한다")
        void refreshTokenIgnoresInvalidAuthorizationHeader() throws Exception {
            User user = saveUser(Role.OPERATOR, "operator@example.com", PASSWORD);
            String refreshToken = dataValue(login(user), "refreshToken");

            mockMvc.perform(MockMvcRequestBuilders.post("/api/token/refresh")
                            .header(HttpHeaders.AUTHORIZATION, bearer("invalid-token"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("refreshToken", refreshToken))))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON200"))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.data.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("위조된 Access Token은 보호 API에서 401-001을 반환한다")
        void tamperedAccessTokenFails() throws Exception {
            User admin = saveUser(Role.ADMIN, "admin@example.com", PASSWORD);
            String accessToken = loginAndGetAccessToken(admin);
            String tamperedToken = accessToken.substring(0, accessToken.length() - 2) + "xx";

            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(tamperedToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("operator01@example.com", "operator1234!", "operator1234!", Role.OPERATOR)))
                    .andExpect(MockMvcResultMatchers.status().isUnauthorized())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("401-001"));
        }

        @Test
        @DisplayName("기존 Access Token은 계정 상태 변경 후에도 만료 전까지 클레임 기반으로 동작한다")
        void existingAccessTokenUsesClaimsUntilItExpires() throws Exception {
            User admin = saveUser(Role.ADMIN, "admin@example.com", PASSWORD);
            String accessToken = loginAndGetAccessToken(admin);
            admin.suspend();
            userRepository.save(admin);

            mockMvc.perform(MockMvcRequestBuilders.post("/api/admin/users")
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createUserJson("operator01@example.com", "operator1234!", "operator1234!", Role.OPERATOR)))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON200"));
        }
    }

    @Nested
    @DisplayName("주문 등록 권한")
    class OrderCreateAuthorization {

        @ParameterizedTest
        @EnumSource(value = Role.class, names = {"EXECUTIVE", "MANUFACTURING_MANAGER"})
        @DisplayName("경영진과 생산관리자는 주문 등록 API에 접근할 수 있다")
        void executiveAndManufacturingManagerCanAccessOrderCreate(Role role) throws Exception {
            String accessToken = loginAndGetAccessToken(saveUser(role, role.name().toLowerCase() + "@example.com", PASSWORD));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("400-001"));
        }

        @ParameterizedTest
        @EnumSource(value = Role.class, names = {"EXECUTIVE", "MANUFACTURING_MANAGER"}, mode = Mode.EXCLUDE)
        @DisplayName("경영진과 생산관리자가 아닌 사용자는 주문 등록 API에 접근할 수 없다")
        void otherRolesCannotAccessOrderCreate(Role role) throws Exception {
            String accessToken = loginAndGetAccessToken(saveUser(role, role.name().toLowerCase() + "@example.com", PASSWORD));

            mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                            .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(MockMvcResultMatchers.status().isForbidden())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("403"));
        }
    }

    @Nested
    @DisplayName("공개 문서 경로")
    class PublicDocumentation {

        @ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(strings = {
                "/swagger-ui/index.html",
                "/swagger-ui.html",
                "/api/swagger-ui/index.html",
                "/api/swagger-ui.html"
        })
        @DisplayName("Swagger 경로는 잘못된 Authorization 헤더가 있어도 JWT 인증 실패로 막히지 않는다")
        void swaggerPathsIgnoreInvalidAuthorizationHeader(String path) throws Exception {
            MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get(path)
                            .header(HttpHeaders.AUTHORIZATION, bearer("invalid-token")))
                    .andReturn();

            Assertions.assertNotEquals(HttpServletResponse.SC_UNAUTHORIZED, result.getResponse().getStatus());
        }

        @Test
        @DisplayName("/api/v3/api-docs는 인증 없이 OpenAPI JSON을 반환한다")
        void apiPrefixedApiDocsReturnOpenApiJson() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/v3/api-docs")
                            .header(HttpHeaders.AUTHORIZATION, bearer("invalid-token")))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.openapi").exists())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.info.title").value("s_map Server API"));
        }

        @Test
        @DisplayName("/api/swagger-ui/index.html은 인증 없이 Swagger UI HTML을 반환한다")
        void apiPrefixedSwaggerUiReturnsHtml() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.get("/api/swagger-ui/index.html")
                            .header(HttpHeaders.AUTHORIZATION, bearer("invalid-token")))
                    .andExpect(MockMvcResultMatchers.status().isOk())
                    .andExpect(MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
        }
    }

    private User saveUser(Role role, String email, String password) {
        User user = User.builder()
                .name(role.name() + " 사용자")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .department("생산관리팀")
                .companyName("s_map")
                .phoneNumber("010-0000-0000")
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    private MvcResult login(User user) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", user.getEmail(),
                                "password", PASSWORD
                        ))))
                .andExpect(MockMvcResultMatchers.status().isOk())
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
                "companyName", "s_map",
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
        Object data = response.get("data");
        if (!(data instanceof Map<?, ?> dataMap)) {
            throw new IllegalStateException("Response data is not an object");
        }

        Object value = dataMap.get(fieldName);
        if (!(value instanceof String stringValue)) {
            throw new IllegalStateException("Response data." + fieldName + " is not a string");
        }
        return stringValue;
    }
}
