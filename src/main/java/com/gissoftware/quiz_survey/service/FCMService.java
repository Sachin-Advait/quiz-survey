package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.model.OfferModel;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
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

  public void sendNotification(
      String token, String title, String body, String category, String contentId) {
    try {
      Message message =
          Message.builder()
              .setToken(token)
              .setNotification(Notification.builder().setTitle(title).setBody(body).build())
              .putData("category", category)
              .putData("contentId", contentId)
              .build();

      String response = FirebaseMessaging.getInstance().send(message);
      log.info("🔥 FCM sent successfully: {}", response);

    } catch (Exception e) {
      log.error("❌ Error sending FCM notification", e);
    }
  }

  /* ================= TRAINING ================= */
  @Async
  public void notifyTrainingAssigned(String trainingId, String title, List<String> userIds) {
    for (String userId : userIds) {
      userRepository
          .findById(userId)
          .ifPresent(
              user -> {
                if (isTokenInvalid(user)) return;
                sendNotification(
                    user.getFcmToken(),
                    title + " Training Assigned",
                    "A new training has been assigned to you. Please complete it before the due date.",
                    "TRAINING",
                    trainingId);
              });
    }
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
                          sendNotification(user.getFcmToken(), title, body, "QUIZ", quiz.getId());
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
        user.getFcmToken(), "New Offer Available 🎉", offer.getTitle(), "OFFER", offer.getId());
  }

  /* ================= COMMON ================= */
  private boolean isTokenInvalid(UserModel user) {
    return user.getFcmToken() == null || user.getFcmToken().isBlank();
  }
}
