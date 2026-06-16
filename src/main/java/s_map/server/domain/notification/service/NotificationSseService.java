package s_map.server.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import s_map.server.domain.notification.dto.res.NotificationResponse;
import s_map.server.domain.notification.dto.res.NotificationUnreadCountResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class NotificationSseService {

    private static final String EVENT_CONNECTED = "connected";
    private static final String EVENT_NOTIFICATION = "notification";
    private static final String EVENT_UNREAD_COUNT = "unread-count";

    private final Map<Long, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();
    private final long timeoutMillis;

    public NotificationSseService(
            @Value("${app.notification.sse.timeout-ms}") long timeoutMillis
    ) {
        this.timeoutMillis = timeoutMillis;
    }

    public SseEmitter subscribe(Long userId, long unreadCount) {
        SseEmitter emitter = new SseEmitter(timeoutMillis);
        emittersByUserId.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(error -> removeEmitter(userId, emitter));

        sendSafely(
                userId,
                emitter,
                EVENT_CONNECTED,
                new NotificationUnreadCountResponse(unreadCount)
        );

        return emitter;
    }

    public void publishNotification(Long userId, NotificationResponse notification) {
        if (userId == null) {
            publishBroadcast(notification);
            return;
        }

        List<SseEmitter> emitters = emittersByUserId.getOrDefault(userId, List.of());
        for (SseEmitter emitter : emitters) {
            sendSafely(userId, emitter, EVENT_NOTIFICATION, notification);
        }
    }

    public void publishUnreadCount(Long userId, long unreadCount) {
        List<SseEmitter> emitters = emittersByUserId.getOrDefault(userId, List.of());
        NotificationUnreadCountResponse payload = new NotificationUnreadCountResponse(unreadCount);

        for (SseEmitter emitter : emitters) {
            sendSafely(userId, emitter, EVENT_UNREAD_COUNT, payload);
        }
    }

    private void publishBroadcast(NotificationResponse notification) {
        emittersByUserId.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                sendSafely(userId, emitter, EVENT_NOTIFICATION, notification);
            }
        });
    }

    private void sendSafely(
            Long userId,
            SseEmitter emitter,
            String eventName,
            Object payload
    ) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(payload));
        } catch (IOException | RuntimeException exception) {
            removeEmitter(userId, emitter);
            log.debug(
                    "Notification SSE emitter removed after send failure. userId={}, eventName={}",
                    userId,
                    eventName
            );
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUserId.remove(userId);
        }
    }
}
