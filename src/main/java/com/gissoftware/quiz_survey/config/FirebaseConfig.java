package com.gissoftware.quiz_survey.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {

  @PostConstruct
  public void initialize() throws IOException {
    if (FirebaseApp.getApps().isEmpty()) {
      FirebaseOptions options =
          FirebaseOptions.builder()
              .setCredentials(
                  GoogleCredentials.fromStream(
                      getClass().getResourceAsStream("/firebase-service-account.json")))
              .build();

      FirebaseApp.initializeApp(options);
      System.out.println("🔥 Firebase initialized in Spring Boot");
    }
  }
}
