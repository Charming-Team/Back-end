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

    /**
     * 기능: 로그인 사용자의 SSE 연결을 등록하고 최초 미읽음 수를 전송한다.
     *
     * Input:
     * - userId / Long / SSE를 구독할 사용자 ID
     * - unreadCount / long / 연결 직후 전송할 미읽음 알림 수
     *
     * Output:
     * - result / SseEmitter / 등록된 SSE emitter
     */
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

    /**
     * 기능: 특정 사용자 또는 전체 사용자에게 새 알림 SSE 이벤트를 전송한다.
     *
     * Input:
     * - userId / Long / 수신 사용자 ID, null이면 전체 접속 사용자 대상
     * - notification / NotificationResponse / 전송할 알림 payload
     *
     * Output:
     * - none / void / SSE 전송 후 반환 값 없음
     */
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

    /**
     * 기능: 특정 사용자에게 미읽음 알림 수 SSE 이벤트를 전송한다.
     *
     * Input:
     * - userId / Long / 수신 사용자 ID
     * - unreadCount / long / 최신 미읽음 알림 수
     *
     * Output:
     * - none / void / SSE 전송 후 반환 값 없음
     */
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
