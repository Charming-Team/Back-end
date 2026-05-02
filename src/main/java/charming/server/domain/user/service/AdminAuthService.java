package charming.server.domain.user.service;

import charming.server.domain.user.dto.req.AdminUserCreateRequest;
import charming.server.domain.user.dto.res.AdminUserCreateResponse;
import charming.server.domain.user.entity.User;
import charming.server.domain.user.repository.UserRepository;
import charming.server.global.error.CustomException;
import charming.server.global.error.ErrorCode;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminUserCreateResponse createUser(AdminUserCreateRequest request) {
        if (!request.password().equals(request.passwordConfirm())) {
            log.warn("[AdminAuthService] 사용자 생성 실패 reason=password_confirm_mismatch email={}", request.email());
            throw new CustomException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            log.warn("[AdminAuthService] 사용자 생성 실패 reason=duplicate_email email={}", email);
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
        log.info(
                "[AdminAuthService] 사용자 생성 완료 userId={}, email={}, role={}, department={}",
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getPhoneNumber()
        );
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
