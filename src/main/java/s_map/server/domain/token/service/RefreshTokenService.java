package s_map.server.domain.token.service;

import s_map.server.domain.token.entity.RefreshToken;
import s_map.server.domain.token.repository.RefreshTokenRepository;
import s_map.server.domain.user.entity.User;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 기능: Refresh Token 원문을 해시로 변환해 저장한다.
     *
     * Input:
     * - user / User / Refresh Token 소유 사용자
     * - token / String / 저장할 Refresh Token 원문
     * - expirationMillis / long / Refresh Token 만료 시간(ms)
     *
     * Output:
     * - result / void / 반환값 없음, Refresh Token 저장만 수행
     */
    @Transactional
    public void save(User user, String token, long expirationMillis) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(token))
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(expirationMillis)))
                .build();

        refreshTokenRepository.save(refreshToken);
        log.debug(
                "[RefreshTokenService] Refresh Token 저장 userId={}, expiresAt={}",
                user.getId(),
                refreshToken.getExpiresAt()
        );
    }

    /**
     * 기능: Refresh Token을 검증하고 검증에 성공한 토큰을 폐기한다.
     *
     * Input:
     * - token / String / 검증할 Refresh Token 원문
     *
     * Output:
     * - user / User / 토큰 소유 사용자
     * - user.id / Long / 토큰 소유 사용자 ID
     * - user.email / String / 토큰 소유 사용자 이메일
     * - user.role / Role / 토큰 소유 사용자 권한
     */
    @Transactional
    public User validateAndRevoke(String token) {
        String tokenHash = hashToken(token);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("[RefreshTokenService] Refresh Token 검증 실패 reason=not_found");
                    return new CustomException(ErrorCode.INVALID_TOKEN);
                });

        if (refreshToken.isRevoked()) {
            log.warn(
                    "[RefreshTokenService] Refresh Token 검증 실패 reason=already_revoked refreshTokenId={}, userId={}",
                    refreshToken.getId(),
                    refreshToken.getUser().getId()
            );
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (refreshToken.isExpired(now)) {
            log.warn(
                    "[RefreshTokenService] Refresh Token 검증 실패 reason=expired refreshTokenId={}, userId={}",
                    refreshToken.getId(),
                    refreshToken.getUser().getId()
            );
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        int revokedCount = refreshTokenRepository.revokeIfActive(tokenHash, now);
        if (revokedCount != 1) {
            log.warn(
                    "[RefreshTokenService] Refresh Token 검증 실패 reason=concurrent_reuse refreshTokenId={}, userId={}",
                    refreshToken.getId(),
                    refreshToken.getUser().getId()
            );
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        log.debug(
                "[RefreshTokenService] Refresh Token 폐기 refreshTokenId={}, userId={}",
                refreshToken.getId(),
                refreshToken.getUser().getId()
        );
        return refreshToken.getUser();
    }

    /**
     * 기능: 보관 기간이 지난 만료 Refresh Token 이력을 삭제한다.
     *
     * Input:
     * - cutoff / LocalDateTime / 이 시각보다 먼저 만료된 Refresh Token 삭제 기준
     *
     * Output:
     * - deletedCount / int / 삭제된 Refresh Token 수
     */
    @Transactional
    public int deleteExpiredBefore(LocalDateTime cutoff) {
        int deletedCount = refreshTokenRepository.deleteExpiredBefore(cutoff);
        log.info(
                "[RefreshTokenService] 만료 Refresh Token 정리 cutoff={}, deletedCount={}",
                cutoff,
                deletedCount
        );
        return deletedCount;
    }

    /**
     * 기능: Refresh Token 원문을 SHA-256 해시 문자열로 변환한다.
     *
     * Input:
     * - token / String / 해시 처리할 Refresh Token 원문
     *
     * Output:
     * - tokenHash / String / Refresh Token 해시값
     */
    private String hashToken(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] tokenHash = messageDigest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(tokenHash);
        } catch (NoSuchAlgorithmException exception) {
            log.error("[RefreshTokenService] Refresh Token 해시 생성 실패", exception);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
    /**
     * 기능: Refresh Token이 현재 로그인한 사용자의 토큰인지 검증하고 검증에 성공한 토큰을 폐기한다.
     *
     * Input:
     * - token / String / 검증할 Refresh Token 원문
     * - email / String / 현재 로그인한 사용자 이메일
     *
     * Output:
     * - user / User / 토큰 소유 사용자
     * - user.id / Long / 토큰 소유 사용자 ID
     * - user.email / String / 토큰 소유 사용자 이메일
     * - user.role / Role / 토큰 소유 사용자 권한
     */
    @Transactional
    public User validateOwnerAndRevoke(String token, String email) {
        String tokenHash = hashToken(token);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("[RefreshTokenService] Refresh Token 검증 실패 reason=not_found");
                    return new CustomException(ErrorCode.INVALID_TOKEN);
                });

        if (refreshToken.isRevoked()) {
            log.warn(
                    "[RefreshTokenService] Refresh Token 검증 실패 reason=already_revoked refreshTokenId={}, userId={}",
                    refreshToken.getId(),
                    refreshToken.getUser().getId()
            );
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        LocalDateTime now = LocalDateTime.now();
        if (refreshToken.isExpired(now)) {
            log.warn(
                    "[RefreshTokenService] Refresh Token 검증 실패 reason=expired refreshTokenId={}, userId={}",
                    refreshToken.getId(),
                    refreshToken.getUser().getId()
            );
            throw new CustomException(ErrorCode.EXPIRED_TOKEN);
        }

        if (!refreshToken.getUser().getEmail().equals(email)) {
            log.warn(
                    "[RefreshTokenService] Refresh Token 소유자 불일치 tokenUserId={}, tokenUserEmail={}, requestEmail={}",
                    refreshToken.getUser().getId(),
                    refreshToken.getUser().getEmail(),
                    email
            );
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        int revokedCount = refreshTokenRepository.revokeIfActive(tokenHash, now);
        if (revokedCount != 1) {
            log.warn(
                    "[RefreshTokenService] Refresh Token 검증 실패 reason=concurrent_reuse refreshTokenId={}, userId={}",
                    refreshToken.getId(),
                    refreshToken.getUser().getId()
            );
            throw new CustomException(ErrorCode.INVALID_TOKEN);
        }

        log.debug(
                "[RefreshTokenService] Refresh Token 소유자 검증 후 폐기 refreshTokenId={}, userId={}",
                refreshToken.getId(),
                refreshToken.getUser().getId()
        );

        return refreshToken.getUser();
    }

}
