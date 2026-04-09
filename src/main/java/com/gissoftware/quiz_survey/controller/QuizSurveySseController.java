package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.model.OfferModel;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.TrainingMaterial;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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

@RestController
@RequestMapping("/api/user/sse")
public class QuizSurveySseController {

  private static final Logger log = LoggerFactory.getLogger(QuizSurveySseController.class);
  private static final int MAX_TOTAL_CONNECTIONS = 10_000;
  private static final int MAX_CONNECTIONS_PER_USER = 5;
  private static final long EMITTER_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(10);

  private final Map<String, CopyOnWriteArrayList<SseEmitter>> userEmitters =
      new ConcurrentHashMap<>();
  private final AtomicInteger totalConnections = new AtomicInteger(0);
  private ScheduledExecutorService heartbeatScheduler;

  @PostConstruct
  public void startHeartbeat() {
    heartbeatScheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "sse-heartbeat");
              t.setDaemon(true);
              return t;
            });

    heartbeatScheduler.scheduleAtFixedRate(
        () -> {
          userEmitters.forEach(
              (userId, emitters) -> {
                emitters.forEach(
                    emitter -> {
                      try {
                        emitter.send(SseEmitter.event().comment("keep-alive"));
                      } catch (IOException e) {
                        log.debug("Keep-alive failed for user {}", userId);
                      }
                    });
              });
        },
        20,
        20,
        TimeUnit.SECONDS);

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
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Too many active connections.");
    }

    CopyOnWriteArrayList<SseEmitter> emitters =
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

    if (emitters.size() >= MAX_CONNECTIONS_PER_USER) {
      log.warn("Per-user SSE connection limit reached for user: {}", userId);
      throw new ResponseStatusException(
          HttpStatus.TOO_MANY_REQUESTS, "Too many connections for this user.");
    }

    SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);

    // ✅ Register callbacks BEFORE adding to map and BEFORE send
    Runnable cleanup =
        () -> {
          if (!emitters.contains(emitter)) return;
          emitters.remove(emitter);
          totalConnections.decrementAndGet();
          if (emitters.isEmpty()) {
            userEmitters.remove(userId);
          }
          log.info(
              "Emitter removed for user {}. Total connections: {}", userId, totalConnections.get());
        };

    // ✅ THIS WAS MISSING — actually register the callbacks on the emitter
    emitter.onCompletion(
        () -> {
          log.info("onCompletion fired for user {}", userId);
          cleanup.run();
        });
    emitter.onTimeout(
        () -> {
          log.warn("onTimeout fired for user {}", userId);
          cleanup.run();
        });
    emitter.onError(
        e -> {
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
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Failed to establish SSE stream.");
    }

    log.info("Emitter map after subscribe: {}", userEmitters.keySet());
    return emitter;
  }

  public void pushNewSurvey(QuizSurveyModel quizSurvey) {
    log.info("Pushing survey. isMandatory: {}", quizSurvey.getIsMandatory());

    if (Boolean.FALSE.equals(quizSurvey.getIsMandatory())) return;

    if (quizSurvey.getTargetedUsers() == null || quizSurvey.getTargetedUsers().isEmpty()) {
      log.warn("No targeted users. Skipping push.");
      return;
    }

    String eventId = quizSurvey.getId() + "-" + System.currentTimeMillis();

    for (String userId : quizSurvey.getTargetedUsers()) {
      CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(userId);

      if (emitters == null || emitters.isEmpty()) continue;

      for (SseEmitter emitter : emitters) {
        try {
          Map<String, Object> payload =
              Map.of(
                  "type", "SURVEY",
                  "id", quizSurvey.getId(),
                  "isMandatory", quizSurvey.getIsMandatory());

          emitter.send(
              SseEmitter.event().id(eventId).name("mandatory").data(payload).reconnectTime(3000));

          log.info("✅ Pushed survey {} to user {}", quizSurvey.getId(), userId);

        } catch (IOException e) {
          log.warn("❌ Failed to push survey to user {}: {}", userId, e.getMessage());
          emitters.remove(emitter);
          totalConnections.decrementAndGet();
        }
      }
    }
  }

  public void pushNewOffer(OfferModel offer) {
    log.info("Pushing offer isMandatory: {}", offer.getIsMandatory());

    if (Boolean.FALSE.equals(offer.getIsMandatory())) return;

    if (offer.getTargetUsers() == null || offer.getTargetUsers().isEmpty()) {
      log.warn("No targeted users in offer. Skipping push.");
      return;
    }

    String eventId = offer.getId() + "-" + System.currentTimeMillis();

    for (String userId : offer.getTargetUsers()) {
      CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(userId);

      if (emitters == null || emitters.isEmpty()) continue;

      for (SseEmitter emitter : emitters) {
        try {
          Map<String, Object> payload =
              Map.of(
                  "type", "OFFER",
                  "id", offer.getId(),
                  "isMandatory", offer.getIsMandatory());

          emitter.send(
              SseEmitter.event().id(eventId).name("mandatory").data(payload).reconnectTime(3000));

          log.info("✅ Pushed offer {} to user {}", offer.getId(), userId);

        } catch (IOException e) {
          log.warn("❌ Failed to push offer to user {}: {}", userId, e.getMessage());
          emitters.remove(emitter);
          totalConnections.decrementAndGet();
        }
      }
    }
  }

  public void pushNewTraining(TrainingMaterial training, List<String> userIds) {
    log.info("Pushing training isMandatory: {}", training.getIsMandatory());

    if (Boolean.FALSE.equals(training.getIsMandatory())) return;

    if (userIds == null || userIds.isEmpty()) {
      log.warn("No targeted users in training. Skipping push.");
      return;
    }

    String eventId = training.getId() + "-" + System.currentTimeMillis();

    for (String userId : userIds) {
      CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(userId);

      if (emitters == null || emitters.isEmpty()) continue;

      for (SseEmitter emitter : emitters) {
        try {
          Map<String, Object> payload =
              Map.of(
                  "type", "TRAINING",
                  "id", training.getId(),
                  "isMandatory", training.getIsMandatory());

          emitter.send(
              SseEmitter.event().id(eventId).name("mandatory").data(payload).reconnectTime(3000));

          log.info("✅ Pushed training {} to user {}", training.getId(), userId);

        } catch (IOException e) {
          log.warn("❌ Failed to push training to user {}: {}", userId, e.getMessage());
          emitters.remove(emitter);
          totalConnections.decrementAndGet();
        }
      }
    }
  }

  private void evictDeadEmitters(String userId, CopyOnWriteArrayList<SseEmitter> emitters) {
    emitters.forEach(
        emitter -> {
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
