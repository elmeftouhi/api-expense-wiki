package com.elmeftouhi.expense.notification.service;

import com.elmeftouhi.expense.notification.dto.NotificationResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class NotificationRealtimeService {

    private static final long SSE_TIMEOUT_MILLIS = 30L * 60L * 1000L;

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long tenantId, Long userId) {
        String streamKey = streamKey(tenantId, userId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        emittersByUser.computeIfAbsent(streamKey, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(streamKey, emitter));
        emitter.onTimeout(() -> removeEmitter(streamKey, emitter));
        emitter.onError(ex -> removeEmitter(streamKey, emitter));

        sendOrRemove(
                streamKey,
                emitter,
                SseEmitter.event()
                        .name("connected")
                        .data(Map.of("connectedAt", Instant.now().toString()))
        );
        return emitter;
    }

    public void publishCreated(Long tenantId, Long userId, NotificationResponse notification) {
        broadcast(
                tenantId,
                userId,
                SseEmitter.event()
                        .name("notification-created")
                        .data(notification)
        );
    }

    public void publishUpdated(Long tenantId, Long userId, NotificationResponse notification) {
        broadcast(
                tenantId,
                userId,
                SseEmitter.event()
                        .name("notification-updated")
                        .data(notification)
        );
    }

    public void publishBulkMarkedAsRead(Long tenantId, Long userId, int changedCount) {
        broadcast(
                tenantId,
                userId,
                SseEmitter.event()
                        .name("notifications-bulk-read")
                        .data(Map.of("changedCount", changedCount, "timestamp", Instant.now().toString()))
        );
    }

    private void broadcast(Long tenantId, Long userId, SseEmitter.SseEventBuilder event) {
        String streamKey = streamKey(tenantId, userId);
        List<SseEmitter> emitters = emittersByUser.get(streamKey);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            sendOrRemove(streamKey, emitter, event);
        }
    }

    private void sendOrRemove(String streamKey, SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        try {
            emitter.send(event);
        } catch (IOException ex) {
            removeEmitter(streamKey, emitter);
            log.debug("Removing stale notification SSE emitter for stream {}", streamKey, ex);
        }
    }

    private void removeEmitter(String streamKey, SseEmitter emitter) {
        emittersByUser.computeIfPresent(streamKey, (key, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }

    private String streamKey(Long tenantId, Long userId) {
        return tenantId + ":" + userId;
    }
}

