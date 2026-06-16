package s_map.server.domain.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import s_map.server.domain.notification.dto.res.NotificationListResponse;
import s_map.server.domain.notification.dto.res.NotificationMutationResponse;
import s_map.server.domain.notification.dto.res.NotificationUnreadCountResponse;
import s_map.server.domain.notification.service.NotificationService;
import s_map.server.domain.user.entity.Role;
import s_map.server.global.common.BaseResponse;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;
import s_map.server.global.security.AuthUser;
import s_map.server.global.security.JwtTokenProvider;

import java.util.Map;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final NotificationService notificationService;
    private final JwtTokenProvider jwtTokenProvider;

    public NotificationController(
            NotificationService notificationService,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.notificationService = notificationService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Operation(
            summary = "알림 목록 조회",
            description = "로그인 사용자의 알림 목록을 최신 생성 시각 기준으로 cursor 방식 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public BaseResponse<NotificationListResponse> getNotifications(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "다음 페이지 조회 기준 알림 ID", example = "991")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 크기. 기본 20, 최대 100", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return BaseResponse.success(notificationService.getNotifications(authUser, cursor, size));
    }

    @Operation(
            summary = "미읽음 알림 수 조회",
            description = "로그인 사용자의 미읽음 알림 배지 수를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "미읽음 알림 수 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/unread-count")
    public BaseResponse<NotificationUnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return BaseResponse.success(notificationService.getUnreadCount(authUser));
    }

    @Operation(
            summary = "알림 SSE 구독",
            description = "로그인 사용자의 실시간 알림 이벤트 스트림을 구독합니다. EventSource 사용 시 token query parameter를 사용할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 연결 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @AuthenticationPrincipal AuthUser authUser,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Parameter(description = "EventSource용 Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
            @RequestParam(required = false) String token
    ) {
        return notificationService.subscribe(resolveAuthUser(authUser, authorizationHeader, token));
    }

    @Operation(
            summary = "알림 단건 읽음 처리",
            description = "로그인 사용자의 특정 알림을 읽음 상태로 변경합니다. 이미 읽은 알림도 정상 처리합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "404", description = "처리할 알림 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PatchMapping("/{notificationId}/read")
    public BaseResponse<NotificationMutationResponse> markRead(
            @AuthenticationPrincipal AuthUser authUser,
            @Parameter(description = "알림 ID", example = "1001")
            @PathVariable Long notificationId
    ) {
        return BaseResponse.success(notificationService.markRead(authUser, notificationId));
    }

    @Operation(
            summary = "알림 전체 읽음 처리",
            description = "로그인 사용자의 미읽음 알림을 모두 읽음 상태로 변경합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 전체 읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PatchMapping("/read-all")
    public BaseResponse<NotificationMutationResponse> markAllRead(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return BaseResponse.success(notificationService.markAllRead(authUser));
    }

    @Operation(
            summary = "알림 전체 삭제",
            description = "로그인 사용자의 알림을 전체 삭제합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 전체 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @DeleteMapping
    public BaseResponse<NotificationMutationResponse> deleteAll(
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return BaseResponse.success(notificationService.deleteAll(authUser));
    }

    private AuthUser resolveAuthUser(
            AuthUser authUser,
            String authorizationHeader,
            String token
    ) {
        if (authUser != null) {
            return authUser;
        }

        String accessToken = resolveToken(authorizationHeader, token);
        Map<String, Object> claims = jwtTokenProvider.getAccessTokenClaims(accessToken);
        Long userId = jwtTokenProvider.getUserId(claims);
        String email = jwtTokenProvider.getSubject(claims);
        Role role = jwtTokenProvider.getRole(claims);

        return new AuthUser(userId, email, role);
    }

    private static String resolveToken(
            String authorizationHeader,
            String token
    ) {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }

        if (token != null && !token.isBlank()) {
            return token.trim();
        }

        throw new CustomException(ErrorCode.UNAUTHORIZED);
    }
}
