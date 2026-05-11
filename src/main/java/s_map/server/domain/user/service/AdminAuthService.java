package s_map.server.domain.user.service;

import s_map.server.domain.user.dto.req.AdminUserCreateRequest;
import s_map.server.domain.user.dto.res.AdminUserCreateResponse;
import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import s_map.server.domain.user.dto.res.AdminUserResponse;
import s_map.server.domain.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 기능: ADMIN 권한 사용자가 신규 사용자를 생성한다.
     *
     * Input:
     * - request / AdminUserCreateRequest / 사용자 생성 요청 값
     * - request.name / String / 사용자 이름
     * - request.email / String / 사용자 이메일
     * - request.password / String / 사용자 비밀번호
     * - request.passwordConfirm / String / 사용자 비밀번호 확인
     * - request.role / Role / 사용자 권한
     * - request.department / String / 소속 부서
     * - request.companyName / String / 회사명
     * - request.phoneNumber / String / 연락처
     *
     * Output:
     * - response / AdminUserCreateResponse / 사용자 생성 응답 값
     * - response.id / Long / 생성된 사용자 ID
     * - response.name / String / 생성된 사용자 이름
     * - response.email / String / 생성된 사용자 이메일
     * - response.role / Role / 생성된 사용자 권한
     * - response.department / String / 생성된 사용자 소속 부서
     * - response.companyName / String / 생성된 사용자 회사명
     * - response.phoneNumber / String / 생성된 사용자 연락처
     */
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
                .companyName(request.companyName().trim())
                .phoneNumber(request.phoneNumber().trim())
                .build();

        User savedUser = userRepository.save(user);
        log.info(
                "[AdminAuthService] 사용자 생성 완료 userId={}, email={}, role={}, department={}, companyName={}",
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getDepartment(),
                savedUser.getCompanyName()
        );
        return new AdminUserCreateResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                savedUser.getRole(),
                savedUser.getDepartment(),
                savedUser.getCompanyName(),
                savedUser.getPhoneNumber()
        );
    }

    /**
     * 기능: 관리자 화면에서 사용자 목록을 조회한다.
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(int page, int size) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<User> users = userRepository.findByStatusNot(UserStatus.WITHDRAWN, pageable);

        return users.map(AdminUserResponse::from);
    }

    /**
     * 기능: 관리자 화면에서 사용자를 삭제 처리한다.
     * 실제 DB 삭제가 아니라 계정 상태를 WITHDRAWN으로 변경한다.
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[AdminAuthService] 사용자 삭제 실패 reason=user_not_found userId={}", userId);
                    return new CustomException(ErrorCode.NOT_FOUND);
                });

        user.withdraw();

        log.info(
                "[AdminAuthService] 사용자 삭제 처리 완료 userId={}, email={}, status={}",
                user.getId(),
                user.getEmail(),
                user.getStatus()
        );
    }
}
