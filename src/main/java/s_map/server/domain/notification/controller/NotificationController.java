package s_map.server.domain.notification.controller;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import s_map.server.domain.notification.dto.res.NotificationListResponse;
import s_map.server.domain.notification.dto.res.NotificationMutationResponse;
import s_map.server.domain.notification.dto.res.NotificationSseTicketResponse;
import s_map.server.domain.notification.dto.res.NotificationUnreadCountResponse;
import s_map.server.domain.notification.service.NotificationService;
import s_map.server.global.common.BaseResponse;
import s_map.server.global.security.AuthUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Notification", description = "알림 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "알림 목록 조회",
            description = "로그인 사용자의 알림 목록을 최신 생성 시각 기준으로 cursor 방식 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping
    public BaseResponse<NotificationListResponse> getNotifications(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser,

            @Parameter(
                    description = "다음 페이지 조회 기준 알림 ID. 첫 조회 시 생략합니다.",
                    example = "991"
            )
            @RequestParam(required = false) Long cursor,

            @Parameter(
                    description = "조회 크기. 기본값은 20이며 최대 100까지 허용합니다.",
                    schema = @Schema(defaultValue = "20", minimum = "1", maximum = "100", example = "20")
            )
            @RequestParam(defaultValue = "20") int size
    ) {
        return BaseResponse.success(
                notificationService.getNotifications(authUser, cursor, size)
        );
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
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return BaseResponse.success(
                notificationService.getUnreadCount(authUser)
        );
    }

    @Operation(
            summary = "알림 SSE 구독",
            description = "로그인 사용자의 실시간 알림 이벤트 스트림을 구독합니다. EventSource 사용 시 사전에 발급받은 1회용 ticket을 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE 연결 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 SSE ticket"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @Parameter(
                    description = "EventSource 연결에 사용할 1회용 구독 ticket",
                    required = true,
                    example = "eyJhbGciOiJIUzI1NiJ9..."
            )
            @RequestParam String ticket
    ) {
        return notificationService.subscribe(ticket);
    }

    @Operation(
            summary = "알림 SSE ticket 발급",
            description = "EventSource 연결에 사용할 짧은 수명의 1회용 구독 ticket을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE ticket 발급 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PostMapping("/sse-ticket")
    public BaseResponse<NotificationSseTicketResponse> issueSseTicket(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return BaseResponse.success(
                notificationService.issueSseTicket(authUser)
        );
    }

    @Operation(
            summary = "알림 단건 읽음 처리",
            description = "로그인 사용자의 특정 알림을 읽음 상태로 변경합니다. 이미 읽은 알림도 정상 처리합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "본인 알림이 아니므로 처리 불가"),
            @ApiResponse(responseCode = "404", description = "처리할 알림 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @PatchMapping("/{notificationId}/read")
    public BaseResponse<NotificationMutationResponse> markRead(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser,

            @Parameter(
                    description = "읽음 처리할 알림 ID",
                    required = true,
                    example = "1001"
            )
            @PathVariable Long notificationId
    ) {
        return BaseResponse.success(
                notificationService.markRead(authUser, notificationId)
        );
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
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return BaseResponse.success(
                notificationService.markAllRead(authUser)
        );
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
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthUser authUser
    ) {
        return BaseResponse.success(
                notificationService.deleteAll(authUser)
        );
    }
}