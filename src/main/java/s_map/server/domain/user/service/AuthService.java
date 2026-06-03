package s_map.server.domain.user.service;

import s_map.server.domain.token.service.RefreshTokenService;
import s_map.server.domain.user.dto.req.LoginRequest;
import s_map.server.domain.user.dto.res.LoginResponse;
import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.domain.user.dto.res.AuthMeResponse;
import s_map.server.domain.user.dto.req.LogoutRequest;

import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import s_map.server.global.security.JwtTokenProvider;
import java.util.Locale;
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
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[AuthService] 로그인 실패 reason=user_not_found email={}", email);
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
            log.warn(
                    "[AuthService] 로그인 실패 reason=password_mismatch userId={}, email={}",
                    user.getId(),
                    user.getEmail()
            );
            throw new CustomException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

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

    /**
     * 기능: 현재 로그인한 사용자의 정보를 조회한다.
     *
     * Input:
     * - email / String / 현재 인증된 사용자의 이메일
     *
     * Output:
     * - response / AuthMeResponse / 현재 로그인 사용자 정보 응답 값
     * - response.id / Long / 사용자 ID
     * - response.name / String / 사용자 이름
     * - response.email / String / 사용자 이메일
     * - response.role / String / 사용자 권한
     * - response.status / String / 사용자 계정 상태
     * - response.department / String / 사용자 소속 부서
     * - response.companyName / String / 사용자 소속 회사명
     * - response.phoneNumber / String / 사용자 연락처
     */
    @Transactional(readOnly = true)
    public AuthMeResponse getMyInfo(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("[AuthService] 내 정보 조회 실패 reason=not_found email={}", email);
                    return new CustomException(ErrorCode.NOT_FOUND);
                });

        return AuthMeResponse.from(user);
    }

    /**
     * 기능: 현재 로그인한 사용자의 Refresh Token인지 검증한 뒤 폐기하여 로그아웃 처리한다.
     *
     * Input:
     * - request / LogoutRequest / 로그아웃 요청 값
     * - request.refreshToken / String / 폐기할 Refresh Token 원문
     * - email / String / 현재 로그인한 사용자 이메일
     *
     * Output:
     * - result / void / 반환값 없음, Refresh Token 폐기만 수행
     */
    @Transactional
    public void logout(LogoutRequest request, String email) {
        refreshTokenService.revokeOwnedTokenForLogout(request.getRefreshToken(), email);

        log.info("[AuthService] 로그아웃 성공 email={}", email);
    }

}
