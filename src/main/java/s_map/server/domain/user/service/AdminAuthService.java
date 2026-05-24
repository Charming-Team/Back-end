package s_map.server.domain.user.service;

import s_map.server.domain.user.dto.req.AdminUserCreateRequest;
import s_map.server.domain.user.dto.res.AdminUserCreateResponse;
import s_map.server.domain.user.dto.res.AdminUserResponse;
import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.entity.UserStatus;
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import s_map.server.domain.user.entity.Role;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

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
     * 기능: 관리자 화면에서 사용자 목록을 페이지 단위로 조회한다.
     * WITHDRAWN 상태의 사용자는 목록에서 제외한다.
     * page와 size 값은 허용 범위 내로 보정한다.
     *
     * Input:
     * - page / int / 조회할 페이지 번호
     * - size / int / 한 페이지에 조회할 사용자 수
     *
     * Output:
     * - response / Page<AdminUserResponse> / 사용자 목록 페이지 응답 값
     * - response.content / List<AdminUserResponse> / 사용자 목록
     * - response.content.id / Long / 사용자 ID
     * - response.content.name / String / 사용자 이름
     * - response.content.email / String / 사용자 이메일
     * - response.content.role / String / 사용자 권한
     * - response.content.status / String / 사용자 계정 상태
     * - response.content.department / String / 사용자 소속 부서
     * - response.content.companyName / String / 사용자 소속 회사명
     * - response.content.phoneNumber / String / 사용자 연락처
     * - response.pageable / Pageable / 페이지 요청 정보
     * - response.totalElements / long / 전체 사용자 수
     * - response.totalPages / int / 전체 페이지 수
     */
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> getUsers(int page, int size) {
        int safePage = Math.max(page, DEFAULT_PAGE);
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<User> users = userRepository.findByStatusNot(UserStatus.WITHDRAWN, pageable);

        return users.map(AdminUserResponse::from);
    }

    /**
     * 기능: 관리자 화면에서 사용자를 삭제 처리한다.
     * 실제 DB 삭제가 아니라 사용자 계정 상태를 WITHDRAWN으로 변경한다.
     * 자기 자신, 다른 ADMIN 계정, 이미 WITHDRAWN 상태인 사용자는 삭제할 수 없다.
     *
     * Input:
     * - userId / Long / 삭제 처리할 사용자 ID
     * - currentAdminId / Long / 현재 로그인한 관리자 ID
     *
     * Output:
     * - result / void / 반환값 없음, 사용자 상태 변경만 수행
     */
    @Transactional
    public void deleteUser(Long userId, Long currentAdminId) {
        try {
            deleteUserInternal(userId, currentAdminId);
        } catch (CustomException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            log.error(
                    "[AdminAuthService] 사용자 삭제 처리 DB 오류 userId={}, adminId={}",
                    userId,
                    currentAdminId,
                    exception
            );
            throw new CustomException(ErrorCode.USER_DELETE_FAILED);
        } catch (RuntimeException exception) {
            log.error(
                    "[AdminAuthService] 사용자 삭제 처리 실패 userId={}, adminId={}",
                    userId,
                    currentAdminId,
                    exception
            );
            throw new CustomException(ErrorCode.USER_DELETE_FAILED);
        }
    }

    private void deleteUserInternal(Long userId, Long currentAdminId) {
        User currentAdmin = userRepository.findById(currentAdminId)
                .orElseThrow(() -> {
                    log.warn(
                            "[AdminAuthService] 사용자 삭제 실패 reason=current_admin_not_found adminId={}",
                            currentAdminId
                    );
                    return new CustomException(ErrorCode.USER_DELETE_FORBIDDEN);
                });

        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[AdminAuthService] 사용자 삭제 실패 reason=user_not_found userId={}", userId);
                    return new CustomException(ErrorCode.USER_DELETE_NOT_ALLOWED);
                });

        if (currentAdmin.getId().equals(targetUser.getId())) {
            log.warn(
                    "[AdminAuthService] 사용자 삭제 실패 reason=self_delete_not_allowed userId={}, email={}",
                    currentAdmin.getId(),
                    currentAdmin.getEmail()
            );
            throw new CustomException(ErrorCode.SELF_DELETE_NOT_ALLOWED);
        }

        if (targetUser.getStatus() == UserStatus.WITHDRAWN) {
            log.warn(
                    "[AdminAuthService] 사용자 삭제 실패 reason=already_withdrawn targetUserId={}, targetEmail={}",
                    targetUser.getId(),
                    targetUser.getEmail()
            );
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }

        if (targetUser.getRole() == Role.ADMIN) {
            log.warn(
                    "[AdminAuthService] 사용자 삭제 실패 reason=admin_delete_not_allowed targetUserId={}, targetEmail={}",
                    targetUser.getId(),
                    targetUser.getEmail()
            );
            throw new CustomException(ErrorCode.ADMIN_DELETE_NOT_ALLOWED);
        }

        targetUser.withdraw();
        userRepository.saveAndFlush(targetUser);

        log.info(
                "[AdminAuthService] 사용자 삭제 처리 완료 adminId={}, targetUserId={}, targetEmail={}, status={}",
                currentAdmin.getId(),
                targetUser.getId(),
                targetUser.getEmail(),
                targetUser.getStatus()
        );
    }
}
