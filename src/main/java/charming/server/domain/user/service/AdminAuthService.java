package charming.server.domain.user.service;

import charming.server.domain.user.dto.req.AdminLoginRequest;
import charming.server.domain.user.dto.res.AdminLoginResponse;
import charming.server.domain.user.entity.Role;
import charming.server.domain.user.entity.User;
import charming.server.domain.user.repository.UserRepository;
import charming.server.global.error.CustomException;
import charming.server.global.error.ErrorCode;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
