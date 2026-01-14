package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.model.NotificationModel;
import com.gissoftware.quiz_survey.model.OfferModel;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.NotificationRepository;
import com.gissoftware.quiz_survey.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class FCMService {

  private final UserRepository userRepository;
  private final NotificationRepository notificationRepository;

  public void sendNotification(
      String userId, String token, String title, String body, String category, String contentId) {

    try {
      // 1️⃣ Save notification in MongoDB
      NotificationModel notification =
          NotificationModel.builder()
              .userId(userId)
              .title(title)
              .message(body)
              .category(category)
              .contentId(contentId)
              .read(false)
              .createdAt(Instant.now())
              .build();

      notificationRepository.save(notification);

      // 2️⃣ Send FCM
      Message message =
          Message.builder()
              .setToken(token)
              .setNotification(Notification.builder().setTitle(title).setBody(body).build())
              .putData("category", category)
              .putData("contentId", contentId)
              .build();

      FirebaseMessaging.getInstance().send(message);

    } catch (Exception e) {
      log.error("❌ Error sending FCM notification", e);
    }
  }

  /* ================= TRAINING ================= */
  @Async
  public void notifyTrainingAssigned(String trainingId, String title, List<String> userIds) {

    List<UserModel> users = userRepository.findAllById(userIds);

    users.parallelStream()
        .forEach(
            user -> {
              if (isTokenInvalid(user)) return;

              sendNotification(
                  user.getId(), // ✅ userId
                  user.getFcmToken(), // ✅ token
                  title + " Training Assigned",
                  "Please complete before due date",
                  "TRAINING",
                  trainingId);
            });
  }

  /* ================= QUIZ / SURVEY ================= */
  @Async
  public void notifyQuizSurveyAssigned(QuizSurveyModel quiz) {
    if (quiz.getTargetedUsers() == null) return;

    quiz.getTargetedUsers()
        .forEach(
            userId ->
                userRepository
                    .findById(userId)
                    .ifPresent(
                        user -> {
                          if (isTokenInvalid(user)) return;

                          String title =
                              quiz.getType().equalsIgnoreCase("Quiz")
                                  ? "New Quiz Assigned"
                                  : "New Survey Assigned";

                          String body =
                              "A new "
                                  + quiz.getType().toLowerCase()
                                  + " has been assigned to you: "
                                  + quiz.getTitle();

                          sendNotification(
                              user.getId(), // ✅
                              user.getFcmToken(), // ✅
                              title,
                              body,
                              "QUIZ",
                              quiz.getId());
                        }));
  }

  /* ================= OFFER ================= */
  @Async
  public void notifyOfferCreated(OfferModel offer) {
    if (offer.getTargetUsers().contains("ALL")) {
      userRepository.findAll().forEach(user -> sendOffer(user, offer));
    } else {
      offer
          .getTargetUsers()
          .forEach(
              userId -> userRepository.findById(userId).ifPresent(user -> sendOffer(user, offer)));
    }
  }

  private void sendOffer(UserModel user, OfferModel offer) {
    if (isTokenInvalid(user)) return;

    sendNotification(
        user.getId(), // ✅
        user.getFcmToken(), // ✅
        "New Offer Available 🎉",
        offer.getTitle(),
        "OFFER",
        offer.getId());
  }

  /* ================= COMMON ================= */
  private boolean isTokenInvalid(UserModel user) {
    return user.getFcmToken() == null || user.getFcmToken().isBlank();
  }
}
