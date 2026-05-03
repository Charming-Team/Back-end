package charming.server.global.security;

import charming.server.domain.user.entity.User;
import charming.server.domain.user.entity.Role;
import charming.server.global.error.CustomException;
import charming.server.global.error.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final ObjectMapper objectMapper;
    private final byte[] secretKey;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;

    public JwtTokenProvider(
            ObjectMapper objectMapper,
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMillis,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMillis
    ) {
        this.objectMapper = objectMapper;
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenExpirationMillis = accessTokenExpirationMillis;
        this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
    }

    public String generateAccessToken(User user) {
        return generateToken(user, ACCESS_TOKEN_TYPE, accessTokenExpirationMillis);
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, REFRESH_TOKEN_TYPE, refreshTokenExpirationMillis);
    }

    public Map<String, Object> getAccessTokenClaims(String token) {
        Map<String, Object> claims = getClaims(token);
        validateTokenType(claims, ACCESS_TOKEN_TYPE);
        return claims;
    }

    public Map<String, Object> getRefreshTokenClaims(String token) {
        Map<String, Object> claims = getClaims(token);
        validateTokenType(claims, REFRESH_TOKEN_TYPE);
        return claims;
    }

    public Long getUserId(Map<String, Object> claims) {
        Object userId = claims.get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        throw new CustomException(ErrorCode.INVALID_TOKEN);
    }

    public String getSubject(Map<String, Object> claims) {
        Object subject = claims.get("sub");
        if (subject instanceof String value && !value.isBlank()) {
            return value;
        }
        throw new CustomException(ErrorCode.INVALID_TOKEN);
    }

    public Role getRole(Map<String, Object> claims) {
        Object role = claims.get("role");
        if (!(role instanceof String value) || value.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    public long getAccessTokenExpirationMillis() {
        return accessTokenExpirationMillis;
    }

    public long getRefreshTokenExpirationMillis() {
        return refreshTokenExpirationMillis;
    }

    private String generateToken(User user, String tokenType, long expirationMillis) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(expirationMillis);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getEmail());
        payload.put("userId", user.getId());
        payload.put("name", user.getName());
        payload.put("role", user.getRole().name());
        payload.put("type", tokenType);
        payload.put("jti", UUID.randomUUID().toString());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String unsignedToken = encodedHeader + "." + encodedPayload;
        return unsignedToken + "." + sign(unsignedToken);
    }

    private Map<String, Object> getClaims(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        Map<String, Object> claims = decodePayload(parts[1]);
        validateExpiration(claims);
        return claims;
    }

    private void validateTokenType(Map<String, Object> claims, String expectedTokenType) {
        if (!expectedTokenType.equals(claims.get("type"))) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validateExpiration(Map<String, Object> claims) {
        Object exp = claims.get("exp");
        if (!(exp instanceof Number number)) {
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        if (Instant.now().getEpochSecond() >= number.longValue()) {
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(value);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (JsonProcessingException exception) {
            log.error("[JwtTokenProvider] JWT JSON 인코딩 실패", exception);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> decodePayload(String encodedPayload) {
        try {
            byte[] json = Base64.getUrlDecoder().decode(encodedPayload);
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (IllegalArgumentException | IOException exception) {
            log.warn("[JwtTokenProvider] JWT payload 디코딩 실패 reason=invalid_payload");
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
            byte[] signature = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            log.error("[JwtTokenProvider] JWT 서명 생성 실패", exception);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < expectedBytes.length; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }
}
