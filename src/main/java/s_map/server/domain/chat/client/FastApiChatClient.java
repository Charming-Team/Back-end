package s_map.server.domain.chat.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import s_map.server.domain.chat.dto.req.FastApiChatAnswerRequest;
import s_map.server.domain.chat.dto.res.ChatAnswerResponse;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiChatClient {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final FastApiChatProperties properties;
    private final ObjectMapper objectMapper;

    public ChatAnswerResponse requestAnswer(FastApiChatAnswerRequest request) {
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        HttpRequest httpRequest = buildRequest(request, timeout);
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();

        log.info(
                "[FastApiChatClient] FastAPI 챗봇 요청 시작 uri={}, sessionId={}, messageId={}, userId={}",
                httpRequest.uri(),
                request.sessionId(),
                request.messageId(),
                request.user().userId()
        );
        try {
            HttpResponse<String> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (isSuccessful(response.statusCode())) {
                log.info(
                        "[FastApiChatClient] FastAPI 챗봇 응답 성공 status={}, uri={}, sessionId={}, messageId={}",
                        response.statusCode(),
                        httpRequest.uri(),
                        request.sessionId(),
                        request.messageId()
                );
                return parseResponse(response.body());
            }
            throw mapFailedResponse(response.statusCode(), response.body(), request);
        } catch (HttpTimeoutException exception) {
            log.warn(
                    "[FastApiChatClient] FastAPI 챗봇 응답 timeout uri={}, sessionId={}, messageId={}",
                    httpRequest.uri(),
                    request.sessionId(),
                    request.messageId()
            );
            throw new CustomException(ErrorCode.CHAT_FASTAPI_TIMEOUT);
        } catch (IOException exception) {
            log.warn(
                    "[FastApiChatClient] FastAPI 챗봇 연결 실패 uri={}, sessionId={}, messageId={}",
                    httpRequest.uri(),
                    request.sessionId(),
                    request.messageId(),
                    exception
            );
            throw new CustomException(ErrorCode.CHAT_FASTAPI_CONNECTION_FAILED);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn(
                    "[FastApiChatClient] FastAPI 챗봇 호출 중 interrupt uri={}, sessionId={}, messageId={}",
                    httpRequest.uri(),
                    request.sessionId(),
                    request.messageId()
            );
            throw new CustomException(ErrorCode.CHAT_FASTAPI_CONNECTION_FAILED);
        }
    }

    private HttpRequest buildRequest(FastApiChatAnswerRequest request, Duration timeout) {
        try {
            return HttpRequest.newBuilder(resolveAnswerUri())
                    .version(HttpClient.Version.HTTP_1_1)
                    .timeout(timeout)
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header(INTERNAL_TOKEN_HEADER, properties.getChatAnswerInternalToken())
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(request),
                            StandardCharsets.UTF_8
                    ))
                    .build();
        } catch (JsonProcessingException exception) {
            log.error("[FastApiChatClient] FastAPI 챗봇 요청 직렬화 실패", exception);
            throw new CustomException(ErrorCode.CHAT_FASTAPI_BAD_REQUEST);
        }
    }

    private URI resolveAnswerUri() {
        String baseUrl = trimTrailingSlash(properties.getBaseUrl());
        String path = properties.getChatAnswerPath().startsWith("/")
                ? properties.getChatAnswerPath()
                : "/" + properties.getChatAnswerPath();
        return URI.create(baseUrl + path);
    }

    private ChatAnswerResponse parseResponse(String body) {
        try {
            return objectMapper.readValue(body, ChatAnswerResponse.class);
        } catch (JsonProcessingException exception) {
            log.warn("[FastApiChatClient] FastAPI 챗봇 응답 역직렬화 실패", exception);
            throw new CustomException(ErrorCode.CHAT_FASTAPI_INVALID_RESPONSE);
        }
    }

    private CustomException mapFailedResponse(
            int statusCode,
            String body,
            FastApiChatAnswerRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(statusCode);
        log.warn(
                "[FastApiChatClient] FastAPI 챗봇 HTTP 실패 status={}, sessionId={}, messageId={}, userId={}, body={}",
                statusCode,
                request.sessionId(),
                request.messageId(),
                request.user().userId(),
                abbreviate(body)
        );

        if (status == HttpStatus.BAD_REQUEST) {
            return new CustomException(ErrorCode.CHAT_FASTAPI_BAD_REQUEST);
        }
        if (status == HttpStatus.FORBIDDEN) {
            return new CustomException(ErrorCode.CHAT_FASTAPI_FORBIDDEN);
        }
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return new CustomException(ErrorCode.CHAT_FASTAPI_UNAVAILABLE);
        }
        if (status != null && status.is5xxServerError()) {
            return new CustomException(ErrorCode.CHAT_FASTAPI_INTERNAL_ERROR);
        }
        return new CustomException(ErrorCode.CHAT_FASTAPI_CONNECTION_FAILED);
    }

    private boolean isSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 500
                ? value
                : value.substring(0, 500) + "...";
    }
}
