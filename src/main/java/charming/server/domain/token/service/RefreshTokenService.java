package charming.server.domain.token.service;

import charming.server.domain.token.entity.RefreshToken;
import charming.server.domain.token.repository.RefreshTokenRepository;
import charming.server.domain.user.entity.User;
import charming.server.global.error.CustomException;
import charming.server.global.error.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void save(User user, String token, long expirationMillis) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(expirationMillis)))
                .build();

        refreshTokenRepository.save(refreshToken);
        log.debug(
                "[RefreshTokenService] Refresh Token 저장 userId={}, expiresAt={}",
                user.getId(),
                refreshToken.getExpiresAt()
        );
    }

    @Transactional
    public User validateAndRevoke(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> {
                    log.warn("[RefreshTokenService] Refresh Token 검증 실패 reason=not_found_or_revoked");
                    return new CustomException(ErrorCode.INVALID_TOKEN);
                });

        if (refreshToken.isExpired(LocalDateTime.now())) {
            refreshToken.revoke();
            log.warn(
                    "[RefreshTokenService] Refresh Token 검증 실패 reason=expired refreshTokenId={}, userId={}",
                    refreshToken.getId(),
                    refreshToken.getUser().getId()
            );
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        refreshToken.revoke();
        log.debug(
                "[RefreshTokenService] Refresh Token 폐기 refreshTokenId={}, userId={}",
                refreshToken.getId(),
                refreshToken.getUser().getId()
        );
        return refreshToken.getUser();
    }
}
