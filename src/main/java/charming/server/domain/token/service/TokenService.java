package charming.server.domain.token.service;

import charming.server.domain.token.dto.req.TokenRefreshRequest;
import charming.server.domain.token.dto.res.TokenRefreshResponse;
import charming.server.domain.user.entity.User;
import charming.server.global.error.CustomException;
import charming.server.global.error.ErrorCode;
import charming.server.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        jwtTokenProvider.getRefreshTokenClaims(request.refreshToken());
        User user = refreshTokenService.validateAndRevoke(request.refreshToken());

        if (!user.isActive()) {
            log.warn(
                    "[TokenService] 토큰 재발급 실패 reason=inactive_account userId={}, email={}, role={}",
                    user.getId(),
                    user.getEmail(),
                    user.getRole()
            );
            throw new CustomException(ErrorCode.INACTIVE_ACCOUNT);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        refreshTokenService.save(user, refreshToken, jwtTokenProvider.getRefreshTokenExpirationMillis());
        log.info(
                "[TokenService] 토큰 재발급 완료 userId={}, email={}, role={}",
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return new TokenRefreshResponse(
                "Bearer",
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationMillis(),
                jwtTokenProvider.getRefreshTokenExpirationMillis()
        );
    }
}
