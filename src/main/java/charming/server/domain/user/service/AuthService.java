package charming.server.domain.user.service;

import charming.server.domain.token.service.RefreshTokenService;
import charming.server.domain.user.dto.req.LoginRequest;
import charming.server.domain.user.dto.res.LoginResponse;
import charming.server.domain.user.entity.User;
import charming.server.domain.user.repository.UserRepository;
import charming.server.global.error.CustomException;
import charming.server.global.error.ErrorCode;
import charming.server.global.security.JwtTokenProvider;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * 기능: 이메일과 비밀번호로 사용자를 인증하고 Access Token, Refresh Token을 발급한다.
     *
     * Input:
     * - request / LoginRequest / 로그인 요청 값
     * - request.email / String / 로그인 이메일
     * - request.password / String / 로그인 비밀번호
     *
     * Output:
     * - response / LoginResponse / 로그인 응답 값
     * - response.id / Long / 사용자 ID
     * - response.name / String / 사용자 이름
     * - response.email / String / 사용자 이메일
     * - response.role / Role / 사용자 권한
     * - response.tokenType / String / 토큰 타입
     * - response.accessToken / String / Access Token
     * - response.refreshToken / String / Refresh Token
     * - response.accessTokenExpiresIn / long / Access Token 만료 시간(ms)
     * - response.refreshTokenExpiresIn / long / Refresh Token 만료 시간(ms)
     */
    @Transactional(noRollbackFor = CustomException.class)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("[AuthService] 로그인 실패 reason=user_not_found email={}", request.email());
                    return new CustomException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
                });

        if (!user.isActive()) {
            log.warn(
                    "[AuthService] 로그인 실패 reason=inactive_account userId={}, email={}, role={}",
                    user.getId(),
                    user.getEmail(),
                    user.getRole()
            );
            throw new CustomException(ErrorCode.INACTIVE_ACCOUNT);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            user.increaseLoginFailCount();
            log.warn(
                    "[AuthService] 로그인 실패 reason=password_mismatch userId={}, email={}, loginFailCount={}",
                    user.getId(),
                    user.getEmail(),
                    user.getLoginFailCount()
            );
            throw new CustomException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        user.recordLoginSuccess(LocalDateTime.now());
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        refreshTokenService.save(user, refreshToken, jwtTokenProvider.getRefreshTokenExpirationMillis());
        log.info(
                "[AuthService] 로그인 성공 userId={}, email={}, role={}",
                user.getId(),
                user.getEmail(),
                user.getRole()
        );

        return new LoginResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                "Bearer",
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationMillis(),
                jwtTokenProvider.getRefreshTokenExpirationMillis()
        );
    }

}
