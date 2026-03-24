package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.PushQuizSurveyMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/user/sse")
public class QuizSurveySseController {

    private static final Logger log = LoggerFactory.getLogger(QuizSurveySseController.class);
    private static final int MAX_TOTAL_CONNECTIONS = 10_000;
    private static final int MAX_CONNECTIONS_PER_USER = 5;
    private static final long EMITTER_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(10);

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    private final AtomicInteger totalConnections = new AtomicInteger(0);
    private ScheduledExecutorService heartbeatScheduler;

    @PostConstruct
    public void startHeartbeat() {
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-heartbeat");
            t.setDaemon(true);
            return t;
        });

        heartbeatScheduler.scheduleAtFixedRate(() -> {
            userEmitters.forEach((userId, emitters) -> {
                emitters.forEach(emitter -> {
                    try {
                        emitter.send(SseEmitter.event().comment("keep-alive"));
                    } catch (IOException e) {
                        log.debug("Keep-alive failed for user {}", userId);
                    }
                });
            });
        }, 20, 20, TimeUnit.SECONDS);

        log.info("SSE heartbeat scheduler started.");
    }

    @PreDestroy
    public void stopHeartbeat() {
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
    }

    @GetMapping(value = "/subscribe/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable String userId) {

        if (totalConnections.get() >= MAX_TOTAL_CONNECTIONS) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Too many active connections.");
        }

        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.computeIfAbsent(
                userId, k -> new CopyOnWriteArrayList<>()
        );

        if (emitters.size() >= MAX_CONNECTIONS_PER_USER) {
            log.warn("Per-user SSE connection limit reached for user: {}", userId);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many connections for this user.");
        }

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

        // ✅ Register callbacks BEFORE adding to map and BEFORE send
        Runnable cleanup = () -> {
            if (!emitters.contains(emitter)) return;
            emitters.remove(emitter);
            totalConnections.decrementAndGet();
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
            log.info("Emitter removed for user {}. Total connections: {}", userId, totalConnections.get());
        };

        // ✅ THIS WAS MISSING — actually register the callbacks on the emitter
        emitter.onCompletion(() -> {
            log.info("onCompletion fired for user {}", userId);
            cleanup.run();
        });
        emitter.onTimeout(() -> {
            log.warn("onTimeout fired for user {}", userId);
            cleanup.run();
        });
        emitter.onError(e -> {
            log.warn("onError fired for user {}: {}", userId, e.getMessage());
            cleanup.run();
        });

        emitters.add(emitter);
        totalConnections.incrementAndGet();
        log.info("User {} subscribed. Total connections: {}", userId, totalConnections.get());

        // ✅ Send handshake AFTER callbacks registered
        try {
            emitter.send(SseEmitter.event().name("connected").data("ready"));
            log.info("Handshake sent to user {}", userId);
        } catch (IOException e) {
            log.error("Handshake failed for user {}", userId, e);
            cleanup.run();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to establish SSE stream.");
        }

        log.info("Emitter map after subscribe: {}", userEmitters.keySet());
        return emitter;
    }

    public void pushNewSurvey(String surveyId, Boolean isMandatory, List<String> targetedUsers) {
        log.info("Pushing survey. isMandatory: {}", isMandatory);

        if (Boolean.FALSE.equals(isMandatory)) return;

        if (targetedUsers == null || targetedUsers.isEmpty()) {
            log.warn("No targeted users. Skipping push.");
            return;
        }

        log.info("Connected users: {}", userEmitters.keySet());
        log.info("Target users: {}", targetedUsers);

        PushQuizSurveyMessage message = new PushQuizSurveyMessage(surveyId, isMandatory, targetedUsers);
        String eventId = surveyId + "-" + System.currentTimeMillis();

        for (String userId : targetedUsers) {
            CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(userId);

            if (emitters == null || emitters.isEmpty()) {
                log.warn("No active SSE connection for user {}. Skipping push.", userId);
                continue;
            }

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .id(eventId)
                            .name("quiz-survey")
                            .data(message)
                            .reconnectTime(3000));
                    log.info("✅ Pushed survey {} to user {}", surveyId, userId);
                } catch (IOException e) {
                    log.warn("❌ Failed to push to user {}: {}", userId, e.getMessage());
                    emitters.remove(emitter);
                    totalConnections.decrementAndGet();
                }
            }
        }
    }

    private void evictDeadEmitters(String userId, CopyOnWriteArrayList<SseEmitter> emitters) {
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("probe"));
            } catch (Exception e) {
                log.debug("Evicting dead emitter for user {}", userId);
                emitters.remove(emitter);
                totalConnections.decrementAndGet();
            }
        });
        if (emitters.isEmpty()) {
            userEmitters.remove(userId);
        }
    }

    public int getActiveConnectionCount() {
        return totalConnections.get();
    }

    public int getConnectedUserCount() {
        return userEmitters.size();
    }
}