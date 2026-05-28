package s_map.server.domain.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import s_map.server.domain.token.repository.RefreshTokenRepository;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(initializers = ChatAnswerIntegrationTest.FastApiPropertyInitializer.class)
class ChatAnswerIntegrationTest {

    private static final String PASSWORD = "password1234!";
    private static final String INTERNAL_TOKEN = "test-chat-answer-token";
    private static final ObjectMapper FAST_API_OBJECT_MAPPER = new ObjectMapper();
    private static final HttpServer FAST_API_SERVER = startFastApiServer();
    private static final AtomicReference<FastApiMode> fastApiMode = new AtomicReference<>(FastApiMode.SUCCESS);
    private static final List<CapturedRequest> capturedRequests = new CopyOnWriteArrayList<>();

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
        capturedRequests.clear();
        fastApiMode.set(FastApiMode.SUCCESS);
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterAll
    static void stopFastApiServer() {
        FAST_API_SERVER.stop(0);
    }

    @Test
    @DisplayName("챗봇 답변 API는 로그인 사용자 컨텍스트와 내부 토큰을 FastAPI로 전달한다")
    void answerSendsAuthenticatedUserContextAndInternalToken() throws Exception {
        User user = saveUser(Role.MANUFACTURING_MANAGER, "manager@example.com");
        String accessToken = loginAndGetAccessToken(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat/answer")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "question", "LINE-ABS-01 병목 원인 알려줘",
                                "sessionId", 1,
                                "messageId", 1
                        ))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("COMMON200"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.sessionId").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.messageId").value(1))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.intent").value("LINE_BOTTLENECK"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.answer").value("LINE-ABS-01 병목은 검사 공정 대기시간 증가가 주된 원인입니다."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.securityResult.status").value("PASSED"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.modelResult.usedRdbEvidence").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.modelResult.usedVectorSearch").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.sources[0].source").value("chat_line_bottleneck_evidence_view"));

        CapturedRequest capturedRequest = onlyCapturedRequest();
        Assertions.assertEquals(INTERNAL_TOKEN, capturedRequest.internalToken());
        Assertions.assertNull(capturedRequest.upgradeHeader());

        Map<String, Object> requestBody = readJsonMap(capturedRequest.body());
        Assertions.assertEquals(1, requestBody.get("sessionId"));
        Assertions.assertEquals(1, requestBody.get("messageId"));
        Assertions.assertEquals("LINE-ABS-01 병목 원인 알려줘", requestBody.get("question"));
        Assertions.assertNotNull(requestBody.get("requestedAt"));

        Map<String, Object> requestUser = nestedMap(requestBody, "user");
        Assertions.assertEquals(user.getId().intValue(), requestUser.get("userId"));
        Assertions.assertEquals("MANUFACTURING_MANAGER", requestUser.get("role"));
        Assertions.assertEquals("S-Map", requestUser.get("companyName"));
        Assertions.assertEquals("ACTIVE", requestUser.get("status"));
    }

    @Test
    @DisplayName("OPERATOR 요청도 FastAPI로 정상 전달한다")
    void answerForwardsOperatorRole() throws Exception {
        User user = saveUser(Role.OPERATOR, "operator@example.com");
        String accessToken = loginAndGetAccessToken(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat/answer")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "question", "오늘 자재 부족 알려줘",
                                "sessionId", 10,
                                "messageId", 24
                        ))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true));

        Map<String, Object> requestBody = readJsonMap(onlyCapturedRequest().body());
        Map<String, Object> requestUser = nestedMap(requestBody, "user");
        Assertions.assertEquals("OPERATOR", requestUser.get("role"));
        Assertions.assertEquals("ACTIVE", requestUser.get("status"));
    }

    @Test
    @DisplayName("토큰 발급 후 비활성화된 사용자는 챗봇 답변 API를 호출할 수 없다")
    void answerBlocksInactiveUserBeforeCallingFastApi() throws Exception {
        User user = saveUser(Role.MANUFACTURING_MANAGER, "inactive-manager@example.com");
        String accessToken = loginAndGetAccessToken(user);
        user.suspend();
        userRepository.saveAndFlush(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat/answer")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "question", "오늘 자재 부족 알려줘",
                                "sessionId", 10,
                                "messageId", 24
                        ))))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("403-002"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("사용할 수 없는 계정입니다."));

        Assertions.assertTrue(capturedRequests.isEmpty());
    }

    @Test
    @DisplayName("FastAPI의 BLOCKED_UNAUTHORIZED 응답은 HTTP 실패가 아니라 정상 챗봇 응답으로 내려간다")
    void blockedUnauthorizedSecurityResultIsSuccessfulChatResponse() throws Exception {
        fastApiMode.set(FastApiMode.BLOCKED);
        User user = saveUser(Role.OPERATOR, "blocked-operator@example.com");
        String accessToken = loginAndGetAccessToken(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat/answer")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "question", "이번 달 매출 알려줘",
                                "sessionId", 2,
                                "messageId", 3
                        ))))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.answer").value("요청하신 정보는 현재 권한으로 조회할 수 없습니다."))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.securityResult.status").value("BLOCKED_UNAUTHORIZED"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.securityResult.code").value("CHAT_SECURITY_004"));
    }

    @Test
    @DisplayName("FastAPI timeout은 BaseResponse.fail로 변환한다")
    void timeoutReturnsBaseResponseFail() throws Exception {
        fastApiMode.set(FastApiMode.TIMEOUT);
        User user = saveUser(Role.MANUFACTURING_MANAGER, "timeout-manager@example.com");
        String accessToken = loginAndGetAccessToken(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat/answer")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "question", "느린 질문",
                                "sessionId", 4,
                                "messageId", 5
                        ))))
                .andExpect(MockMvcResultMatchers.status().isGatewayTimeout())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("CHAT_FASTAPI_002"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("챗봇 응답 시간이 초과되었습니다."));
    }

    @ParameterizedTest
    @CsvSource({
            "HTTP_400, 502, CHAT_FASTAPI_003, 챗봇 요청을 처리할 수 없습니다.",
            "HTTP_403, 502, CHAT_FASTAPI_004, 챗봇 서버 요청에 실패했습니다.",
            "HTTP_500, 502, CHAT_FASTAPI_006, 챗봇 서버 내부 오류가 발생했습니다.",
            "HTTP_503, 503, CHAT_FASTAPI_005, 챗봇 서버를 사용할 수 없습니다."
    })
    @DisplayName("FastAPI HTTP 실패는 BaseResponse.fail로 변환한다")
    void fastApiHttpErrorReturnsBaseResponseFail(
            FastApiMode mode,
            int expectedStatus,
            String expectedCode,
            String expectedMessage
    ) throws Exception {
        fastApiMode.set(mode);
        User user = saveUser(Role.EXECUTIVE, "executive@example.com");
        String accessToken = loginAndGetAccessToken(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat/answer")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "question", "라인 병목 알려줘",
                                "sessionId", 7,
                                "messageId", 8
                        ))))
                .andExpect(MockMvcResultMatchers.status().is(expectedStatus))
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(expectedCode))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(expectedMessage));
    }

    @Test
    @DisplayName("FastAPI 응답 추적 ID가 요청과 다르면 잘못된 챗봇 응답으로 처리한다")
    void mismatchedFastApiTrackingIdsReturnInvalidResponse() throws Exception {
        fastApiMode.set(FastApiMode.MISMATCHED_TRACKING_ID);
        User user = saveUser(Role.MANUFACTURING_MANAGER, "mismatch-manager@example.com");
        String accessToken = loginAndGetAccessToken(user);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat/answer")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "question", "라인 병목 알려줘",
                                "sessionId", 7,
                                "messageId", 8
                        ))))
                .andExpect(MockMvcResultMatchers.status().isBadGateway())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value("CHAT_FASTAPI_007"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("챗봇 서버 응답을 처리할 수 없습니다."));
    }

    private static HttpServer startFastApiServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/ai/api/v1/chat/answer", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                capturedRequests.add(new CapturedRequest(
                        exchange.getRequestMethod(),
                        exchange.getRequestURI().getPath(),
                        exchange.getRequestHeaders().getFirst("X-Internal-Token"),
                        exchange.getRequestHeaders().getFirst("Upgrade"),
                        body
                ));

                if (fastApiMode.get() == FastApiMode.TIMEOUT) {
                    sleepPastClientTimeout();
                    writeResponse(exchange, 200, successResponse(body));
                    return;
                }
                if (fastApiMode.get() == FastApiMode.HTTP_400) {
                    writeResponse(exchange, 400, "{\"detail\":\"bad request\"}");
                    return;
                }
                if (fastApiMode.get() == FastApiMode.HTTP_403) {
                    writeResponse(exchange, 403, "{\"detail\":\"forbidden\"}");
                    return;
                }
                if (fastApiMode.get() == FastApiMode.HTTP_500) {
                    writeResponse(exchange, 500, "{\"detail\":\"internal error\"}");
                    return;
                }
                if (fastApiMode.get() == FastApiMode.HTTP_503) {
                    writeResponse(exchange, 503, "{\"detail\":\"unavailable\"}");
                    return;
                }
                if (fastApiMode.get() == FastApiMode.BLOCKED) {
                    writeResponse(exchange, 200, blockedResponse(body));
                    return;
                }
                if (fastApiMode.get() == FastApiMode.MISMATCHED_TRACKING_ID) {
                    CapturedIds ids = capturedIds(body);
                    writeResponse(exchange, 200, successResponse(ids.sessionId() + 1, ids.messageId() + 1));
                    return;
                }
                writeResponse(exchange, 200, successResponse(body));
            });
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return server;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to start FastAPI test server", exception);
        }
    }

    private static void writeResponse(
            com.sun.net.httpserver.HttpExchange exchange,
            int statusCode,
            String responseBody
    ) throws IOException {
        byte[] responseBytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
        exchange.close();
    }

    private static void sleepPastClientTimeout() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static String successResponse(String requestBody) throws IOException {
        CapturedIds ids = capturedIds(requestBody);
        return successResponse(ids.sessionId(), ids.messageId());
    }

    private static String successResponse(
            long sessionId,
            long messageId
    ) {
        return """
                {
                  "sessionId": %d,
                  "messageId": %d,
                  "intent": "LINE_BOTTLENECK",
                  "answer": "LINE-ABS-01 병목은 검사 공정 대기시간 증가가 주된 원인입니다.",
                  "basisTime": "2026-05-24T15:30:00+09:00",
                  "urls": [
                    {
                      "label": "라인 현황",
                      "url": "/lines/101",
                      "type": "LINE"
                    }
                  ],
                  "sources": [
                    {
                      "sourceType": "LINE",
                      "title": "LINE-ABS-01 라인 병목 근거",
                      "summary": "대기시간과 가동률 기준으로 병목 가능성이 확인되었습니다.",
                      "url": "/lines/101",
                      "referenceId": 101,
                      "source": "chat_line_bottleneck_evidence_view",
                      "basisTime": "2026-05-24T15:30:00+09:00",
                      "sourceOrigin": "RDB",
                      "relevanceScore": null
                    }
                  ],
                  "securityResult": {
                    "status": "PASSED",
                    "code": null,
                    "reason": null
                  },
                  "modelResult": {
                    "usedVectorSearch": true,
                    "usedRdbEvidence": true,
                    "usedLlmGeneration": true,
                    "llmCacheHit": false,
                    "llmUsage": {
                      "promptTokens": 1200,
                      "completionTokens": 300,
                      "totalTokens": 1500
                    },
                    "rdbEvidenceCount": 3,
                    "documentSourceCount": 2,
                    "evidenceCount": 5,
                    "vectorSearchSkippedReason": null,
                    "llmGenerationSkippedReason": null
                  }
                }
                """.formatted(sessionId, messageId);
    }

    private static String blockedResponse(String requestBody) throws IOException {
        CapturedIds ids = capturedIds(requestBody);
        return """
                {
                  "sessionId": %d,
                  "messageId": %d,
                  "intent": "FINANCIAL_QUERY",
                  "answer": "요청하신 정보는 현재 권한으로 조회할 수 없습니다.",
                  "basisTime": "2026-05-24T15:30:00+09:00",
                  "urls": [],
                  "sources": [],
                  "securityResult": {
                    "status": "BLOCKED_UNAUTHORIZED",
                    "code": "CHAT_SECURITY_004",
                    "reason": "권한상 답변할 수 없는 질문입니다."
                  },
                  "modelResult": {
                    "usedVectorSearch": false,
                    "usedRdbEvidence": false,
                    "usedLlmGeneration": false,
                    "llmCacheHit": false,
                    "llmUsage": null,
                    "rdbEvidenceCount": 0,
                    "documentSourceCount": 0,
                    "evidenceCount": 0,
                    "vectorSearchSkippedReason": "SECURITY_BLOCKED",
                    "llmGenerationSkippedReason": "SECURITY_BLOCKED"
                  }
                }
                """.formatted(ids.sessionId(), ids.messageId());
    }

    private static CapturedIds capturedIds(String requestBody) throws IOException {
        Map<String, Object> body = FAST_API_OBJECT_MAPPER.readValue(
                requestBody,
                new TypeReference<>() {
                }
        );
        return new CapturedIds(
                ((Number) body.get("sessionId")).longValue(),
                ((Number) body.get("messageId")).longValue()
        );
    }

    private User saveUser(Role role, String email) {
        return userRepository.save(User.builder()
                .name("김길동")
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .role(role)
                .department("생산관리팀")
                .companyName("S-Map")
                .phoneNumber("010-1234-5678")
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

        return dataValue(result, "accessToken");
    }

    private CapturedRequest onlyCapturedRequest() {
        Assertions.assertEquals(1, capturedRequests.size());
        return capturedRequests.getFirst();
    }

    private Map<String, Object> readJsonMap(String value) throws Exception {
        return objectMapper.readValue(value, new TypeReference<>() {
        });
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Map<String, Object> value, String key) {
        return (Map<String, Object>) value.get(key);
    }

    private String dataValue(MvcResult result, String key) throws Exception {
        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
        Map<String, Object> data = nestedMap(body, "data");
        return (String) data.get(key);
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    enum FastApiMode {
        SUCCESS,
        BLOCKED,
        TIMEOUT,
        HTTP_400,
        HTTP_403,
        HTTP_503,
        HTTP_500,
        MISMATCHED_TRACKING_ID
    }

    record CapturedIds(
            long sessionId,
            long messageId
    ) {
    }

    record CapturedRequest(
            String method,
            String path,
            String internalToken,
            String upgradeHeader,
            String body
    ) {
    }

    static class FastApiPropertyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                    "ai.fastapi.base-url=http://127.0.0.1:" + FAST_API_SERVER.getAddress().getPort(),
                    "ai.fastapi.chat-answer-internal-token=" + INTERNAL_TOKEN,
                    "ai.fastapi.timeout-seconds=1"
            ).applyTo(applicationContext.getEnvironment());
        }
    }
}
