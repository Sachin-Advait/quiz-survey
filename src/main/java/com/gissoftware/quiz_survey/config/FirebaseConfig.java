package com.gissoftware.quiz_survey.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {

//    @PostConstruct
//    public void initialize() {
//        try {
//            FileInputStream serviceAccount = new FileInputStream("path-to-firebase-adminsdk.json");
//
//            FirebaseOptions options = FirebaseOptions.builder()
//                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
//                    .build();
//
//            if (FirebaseApp.getApps().isEmpty()) {
//                FirebaseApp.initializeApp(options);
//            }
//        } catch (IOException e) {
//            throw new RuntimeException("Firebase initialization error", e);
//        }
//    }
}
