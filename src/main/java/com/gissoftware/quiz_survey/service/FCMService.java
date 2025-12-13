package com.gissoftware.quiz_survey.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FCMService {

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
}
