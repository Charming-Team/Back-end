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
