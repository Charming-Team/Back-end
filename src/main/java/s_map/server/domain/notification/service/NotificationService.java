package s_map.server.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import s_map.server.domain.notification.dto.req.NotificationCreateRequest;
import s_map.server.domain.notification.dto.res.NotificationListResponse;
import s_map.server.domain.notification.dto.res.NotificationMutationResponse;
import s_map.server.domain.notification.dto.res.NotificationResponse;
import s_map.server.domain.notification.dto.res.NotificationSseTicketResponse;
import s_map.server.domain.notification.dto.res.NotificationUnreadCountResponse;
import s_map.server.domain.notification.entity.NotificationReferenceType;
import s_map.server.domain.notification.entity.NotificationSeverity;
import s_map.server.domain.notification.entity.NotificationType;
import s_map.server.domain.notification.repository.NotificationRepository;
import s_map.server.domain.notification.repository.NotificationRepository.NotificationRow;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.entity.UserStatus;
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import s_map.server.global.security.AuthUser;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationSseService notificationSseService;
    private final NotificationSseTicketService notificationSseTicketService;
    private final UserRepository userRepository;


    /**
     * 기능: 로그인 사용자의 알림 목록을 최신순 cursor 방식으로 조회한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 정보
     * - cursor / Long / 다음 페이지 조회 기준 알림 ID
     * - size / int / 조회할 알림 수
     *
     * Output:
     * - result / NotificationListResponse / 알림 목록, 다음 cursor, 미읽음 알림 수
     */
    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(
            AuthUser authUser,
            Long cursor,
            int size
    ) {
        Long userId = resolveUserId(authUser);
        int normalizedSize = normalizeSize(size);
        List<NotificationRow> rows = notificationRepository.findNotifications(
                userId,
                cursor,
                normalizedSize + 1
        );

        boolean hasNext = rows.size() > normalizedSize;
        List<NotificationResponse> items = rows.stream()
                .limit(normalizedSize)
                .map(this::toResponse)
                .toList();

        Long nextCursor = hasNext && !items.isEmpty()
                ? items.get(items.size() - 1).notificationId()
                : null;

        long unreadCount = notificationRepository.countUnread(userId);

        log.info(
                "Notification list viewed. userId={}, cursor={}, size={}, resultCount={}, hasNext={}",
                userId,
                cursor,
                normalizedSize,
                items.size(),
                hasNext
        );

        return new NotificationListResponse(
                items,
                nextCursor,
                hasNext,
                unreadCount
        );
    }

    /**
     * 기능: 로그인 사용자의 미읽음 알림 수를 조회한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 정보
     *
     * Output:
     * - result / NotificationUnreadCountResponse / 미읽음 알림 수
     */
    @Transactional(readOnly = true)
    public NotificationUnreadCountResponse getUnreadCount(AuthUser authUser) {
        return new NotificationUnreadCountResponse(
                notificationRepository.countUnread(resolveUserId(authUser))
        );
    }

    /**
     * 기능: 로그인 사용자의 알림 SSE 스트림을 구독한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 정보
     *
     * Output:
     * - result / SseEmitter / 알림 이벤트 스트림
     */
    @Transactional(readOnly = true)
    public NotificationSseTicketResponse issueSseTicket(AuthUser authUser) {
        return notificationSseTicketService.issue(resolveUserId(authUser));
    }

    @Transactional(readOnly = true)
    public SseEmitter subscribe(String ticket) {
        Long userId = notificationSseTicketService.consume(ticket);
        long unreadCount = notificationRepository.countUnread(userId);

        return notificationSseService.subscribe(userId, unreadCount);
    }

    /**
     * 기능: 알림을 단건 읽음 처리한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 정보
     * - notificationId / Long / 읽음 처리할 알림 ID
     *
     * Output:
     * - result / NotificationMutationResponse / 처리 건수와 미읽음 알림 수
     */
    @Transactional
    public NotificationMutationResponse markRead(
            AuthUser authUser,
            Long notificationId
    ) {
        Long userId = resolveUserId(authUser);
        validatePositiveId(notificationId, "notificationId");

        notificationRepository.findById(userId, notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        int affectedCount = notificationRepository.markRead(userId, notificationId);
        long unreadCount = notificationRepository.countUnread(userId);
        notificationSseService.publishUnreadCount(userId, unreadCount);

        log.info(
                "Notification marked as read. userId={}, notificationId={}, affectedCount={}",
                userId,
                notificationId,
                affectedCount
        );

        return new NotificationMutationResponse(affectedCount, unreadCount);
    }

    /**
     * 기능: 로그인 사용자의 모든 미읽음 알림을 읽음 처리한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 정보
     *
     * Output:
     * - result / NotificationMutationResponse / 처리 건수와 미읽음 알림 수
     */
    @Transactional
    public NotificationMutationResponse markAllRead(AuthUser authUser) {
        Long userId = resolveUserId(authUser);
        int affectedCount = notificationRepository.markAllRead(userId);
        long unreadCount = notificationRepository.countUnread(userId);
        notificationSseService.publishUnreadCount(userId, unreadCount);

        log.info(
                "All notifications marked as read. userId={}, affectedCount={}",
                userId,
                affectedCount
        );

        return new NotificationMutationResponse(affectedCount, unreadCount);
    }

    /**
     * 기능: 로그인 사용자의 알림을 전체 삭제한다.
     *
     * Input:
     * - authUser / AuthUser / JWT에서 추출한 로그인 사용자 정보
     *
     * Output:
     * - result / NotificationMutationResponse / 삭제 건수와 미읽음 알림 수
     */
    @Transactional
    public NotificationMutationResponse deleteAll(AuthUser authUser) {
        Long userId = resolveUserId(authUser);
        int affectedCount = notificationRepository.deleteAll(userId);
        long unreadCount = notificationRepository.countUnread(userId);
        notificationSseService.publishUnreadCount(userId, unreadCount);

        log.info(
                "All notifications deleted. userId={}, affectedCount={}",
                userId,
                affectedCount
        );

        return new NotificationMutationResponse(affectedCount, unreadCount);
    }

    /**
     * 기능: 시스템 이벤트 기준 알림을 생성하고 접속 중인 사용자에게 SSE로 전송한다.
     *
     * Input:
     * - request / NotificationCreateRequest / 수신자, 유형, 제목, 내용, 참조 대상 정보
     *
     * Output:
     * - result / NotificationResponse / 생성된 알림 정보
     */
    @Transactional
    public NotificationResponse createNotification(NotificationCreateRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        validatePositiveId(request.recipientUserId(), "recipientUserId");

        NotificationRow savedRow = notificationRepository.save(request);
        NotificationResponse response = toResponse(savedRow);

        notificationSseService.publishNotification(request.recipientUserId(), response);
        notificationSseService.publishUnreadCount(
                request.recipientUserId(),
                notificationRepository.countUnread(request.recipientUserId())
        );

        return response;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createReportGeneratedNotification(
            Long recipientUserId,
            Long reportId,
            String reportTitle
    ) {
        String safeTitle = StringUtils.hasText(reportTitle) ? reportTitle : "보고서";

        createNotification(new NotificationCreateRequest(
                recipientUserId,
                NotificationType.REPORT_READY,
                "보고서 생성 완료",
                safeTitle + " 생성이 완료되었습니다.",
                "/reports/" + reportId,
                NotificationSeverity.LOW,
                NotificationReferenceType.REPORT,
                reportId
        ));
    }

    /**
     * 기능: 납기 지연 위험 예측 결과를 생산관리자와 경영진에게 알림으로 저장한다.
     *
     * Input:
     * - orderId / Long / 지연 위험이 예측된 주문 ID
     * - predictionId / Long / AI 예측 결과 ID
     * - riskLevel / String / 예측 위험도
     * - delayProbability / BigDecimal / 예측 지연 확률
     *
     * Output:
     * - none / void / 대상 수신자별 알림 저장 및 SSE 발행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createDelayRiskNotification(
            Long orderId,
            Long predictionId,
            String riskLevel,
            BigDecimal delayProbability
    ) {
        if (!StringUtils.hasText(riskLevel) || "SAFE".equals(riskLevel)) {
            return;
        }

        NotificationSeverity severity = switch (riskLevel) {
            case "CRITICAL" -> NotificationSeverity.HIGH;
            case "WARNING" -> NotificationSeverity.MEDIUM;
            default -> NotificationSeverity.LOW;
        };
        String probabilityText = delayProbability == null
                ? "확인 필요"
                : delayProbability.multiply(BigDecimal.valueOf(100))
                .setScale(1, RoundingMode.HALF_UP)
                + "%";

        notifyRecipients(
                findActiveUserIdsByRoles(List.of(Role.MANUFACTURING_MANAGER, Role.EXECUTIVE)),
                NotificationType.DELAY_RISK,
                "납기 지연 위험 발생",
                "주문 " + orderId + "의 납기 지연 위험이 " + riskLevel
                        + "로 예측되었습니다. 지연 확률: " + probabilityText,
                severity,
                NotificationReferenceType.ORDER,
                orderId
        );

        log.info(
                "Delay risk notification created. orderId={}, predictionId={}, riskLevel={}, delayProbability={}",
                orderId,
                predictionId,
                riskLevel,
                delayProbability
        );
    }

    /**
     * 기능: 단일 생산계획 변경 알림을 담당자, 생산관리자, 경영진에게 저장한다.
     *
     * Input:
     * - planId / Long / 변경된 생산계획 ID
     * - operatorId / Long / 생산 담당자 사용자 ID
     *
     * Output:
     * - none / void / 대상 수신자별 알림 저장 및 SSE 발행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createScheduleAppliedNotification(Long planId, Long operatorId) {
        if (planId == null) {
            return;
        }

        Set<Long> recipientIds = new LinkedHashSet<>(
                findActiveUserIdsByRoles(List.of(Role.MANUFACTURING_MANAGER, Role.EXECUTIVE))
        );
        if (operatorId != null) {
            recipientIds.add(operatorId);
        }

        notifyRecipients(
                recipientIds,
                NotificationType.SCHEDULE_APPLIED,
                "생산계획 변경",
                "생산계획 " + planId + "의 일정 또는 배정 정보가 변경되었습니다.",
                NotificationSeverity.MEDIUM,
                NotificationReferenceType.PLAN,
                planId
        );
    }

    /**
     * 기능: AI 선택안 반영으로 여러 생산계획이 변경된 경우 요약 알림을 저장한다.
     *
     * Input:
     * - appliedBy / Long / AI 선택안을 반영한 사용자 ID
     * - savedPlanIds / Collection<Long> / 반영된 생산계획 ID 목록
     *
     * Output:
     * - none / void / 대상 수신자별 알림 저장 및 SSE 발행
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createScheduleAppliedSummaryNotification(
            Long appliedBy,
            Collection<Long> savedPlanIds
    ) {
        if (savedPlanIds == null || savedPlanIds.isEmpty()) {
            return;
        }

        Long referencePlanId = savedPlanIds.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (referencePlanId == null) {
            return;
        }

        Set<Long> recipientIds = new LinkedHashSet<>(
                findActiveUserIdsByRoles(List.of(Role.MANUFACTURING_MANAGER, Role.EXECUTIVE))
        );
        if (appliedBy != null) {
            recipientIds.add(appliedBy);
        }

        notifyRecipients(
                recipientIds,
                NotificationType.SCHEDULE_APPLIED,
                "AI 생산계획 반영 완료",
                "AI 추천안 반영으로 생산계획 " + savedPlanIds.size() + "건이 변경되었습니다.",
                NotificationSeverity.MEDIUM,
                NotificationReferenceType.PLAN,
                referencePlanId
        );
    }

    private NotificationResponse toResponse(NotificationRow row) {
        String url = StringUtils.hasText(row.url())
                ? row.url()
                : resolveUrl(row.referenceType(), row.referenceId());

        return new NotificationResponse(
                row.notificationId(),
                row.notificationType(),
                row.title(),
                row.content(),
                row.createdAt(),
                row.isRead(),
                url,
                row.severity(),
                row.referenceType(),
                row.referenceId()
        );
    }

    private static String resolveUrl(
            NotificationReferenceType referenceType,
            Long referenceId
    ) {
        if (referenceType == null || referenceId == null) {
            return null;
        }

        return switch (referenceType) {
            case ORDER -> "/risk?orderId=" + referenceId;
            case PREDICTION -> "/risk?predictionId=" + referenceId;
            case MATERIAL -> "/materials/" + referenceId;
            case REPORT -> "/reports/" + referenceId;
            case PLAN -> "/plan?planId=" + referenceId;
            case LINE -> "/lines/" + referenceId;
            case MACHINE -> "/lines?machineId=" + referenceId;
            case SYSTEM -> null;
        };
    }

    private List<Long> findActiveUserIdsByRoles(Collection<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        return userRepository.findByRoleInAndStatus(roles, UserStatus.ACTIVE).stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private void notifyRecipients(
            Collection<Long> recipientIds,
            NotificationType notificationType,
            String title,
            String content,
            NotificationSeverity severity,
            NotificationReferenceType referenceType,
            Long referenceId
    ) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            log.info(
                    "Notification skipped because recipient is empty. notificationType={}, referenceType={}, referenceId={}",
                    notificationType,
                    referenceType,
                    referenceId
            );
            return;
        }

        for (Long recipientId : recipientIds) {
            if (recipientId == null) {
                continue;
            }

            createNotification(new NotificationCreateRequest(
                    recipientId,
                    notificationType,
                    title,
                    content,
                    null,
                    severity,
                    referenceType,
                    referenceId
            ));
        }
    }

    private static Long resolveUserId(AuthUser authUser) {
        if (authUser == null || authUser.id() == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return authUser.id();
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }

        return Math.min(size, MAX_SIZE);
    }

    private static void validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive.");
        }
    }
}
