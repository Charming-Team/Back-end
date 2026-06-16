package s_map.server.domain.notification.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "SSE 구독 ticket 발급 응답")
public record NotificationSseTicketResponse(
        @Schema(description = "EventSource 연결에 사용할 1회용 ticket")
        String ticket,

        @Schema(description = "ticket 만료 시각")
        OffsetDateTime expiresAt
) {
}
