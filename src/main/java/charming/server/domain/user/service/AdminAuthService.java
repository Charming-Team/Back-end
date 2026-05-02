package charming.server.domain.user.service;

import charming.server.domain.user.dto.req.AdminLoginRequest;
import charming.server.domain.user.dto.req.AdminUserCreateRequest;
import charming.server.domain.user.dto.res.AdminLoginResponse;
import charming.server.domain.user.dto.res.AdminUserCreateResponse;
import charming.server.domain.user.entity.Role;
import charming.server.domain.user.entity.User;
import charming.server.domain.user.repository.UserRepository;
import charming.server.global.error.CustomException;
import charming.server.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ADMIN 로그인
    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_LOGIN_CREDENTIALS));

        if (user.getRole() != Role.ADMIN) {
            throw new CustomException(ErrorCode.ADMIN_AUTH_REQUIRED);
        }

        if (!user.isActive()) {
            throw new CustomException(ErrorCode.INACTIVE_ACCOUNT);
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            user.increaseLoginFailCount();
            throw new CustomException(ErrorCode.INVALID_LOGIN_CREDENTIALS);
        }

        user.recordLoginSuccess(LocalDateTime.now());
        return new AdminLoginResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    //ADMIN 회원가입
    @Transactional
    public AdminUserCreateResponse createUser(AdminUserCreateRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .department(request.department().trim())
                .phoneNumber(request.phoneNumber().trim())
                .build();

        User savedUser = userRepository.save(user);
        return new AdminUserCreateResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getDepartment(),
                savedUser.getPhoneNumber()
        );
    }
}
