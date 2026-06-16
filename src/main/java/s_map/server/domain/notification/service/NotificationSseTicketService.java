package s_map.server.domain.notification.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import s_map.server.domain.notification.dto.res.NotificationSseTicketResponse;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationSseTicketService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");
    private static final int TICKET_BYTE_LENGTH = 32;

    private final Map<String, TicketState> tickets = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration ttl;

    public NotificationSseTicketService(
            @Value("${app.notification.sse.ticket-ttl-ms}") long ticketTtlMillis
    ) {
        this.ttl = Duration.ofMillis(ticketTtlMillis);
    }

    /**
     * 기능: EventSource 연결에 사용할 짧은 수명의 1회용 SSE ticket을 발급한다.
     *
     * Input:
     * - userId / Long / ticket을 발급받을 사용자 ID
     *
     * Output:
     * - result / NotificationSseTicketResponse / 발급된 ticket과 만료 시각
     */
    public NotificationSseTicketResponse issue(Long userId) {
        byte[] randomBytes = new byte[TICKET_BYTE_LENGTH];
        secureRandom.nextBytes(randomBytes);

        String ticket = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
        OffsetDateTime expiresAt = OffsetDateTime.now(DEFAULT_ZONE).plus(ttl);

        tickets.put(ticket, new TicketState(userId, expiresAt));
        return new NotificationSseTicketResponse(ticket, expiresAt);
    }

    /**
     * 기능: SSE 연결 요청의 1회용 ticket을 검증하고 사용자 ID를 반환한다.
     *
     * Input:
     * - ticket / String / EventSource 연결에 사용할 1회용 ticket
     *
     * Output:
     * - result / Long / ticket을 발급받은 사용자 ID
     */
    public Long consume(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        TicketState ticketState = tickets.remove(ticket.trim());
        if (ticketState == null || ticketState.isExpired()) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return ticketState.userId();
    }

    private record TicketState(Long userId, OffsetDateTime expiresAt) {

        private boolean isExpired() {
            return OffsetDateTime.now(DEFAULT_ZONE).isAfter(expiresAt);
        }
    }
}
