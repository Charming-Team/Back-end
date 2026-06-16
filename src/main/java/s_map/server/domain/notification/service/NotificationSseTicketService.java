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
