package com.gissoftware.quiz_survey.model;

import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "notifications")
@CompoundIndexes({
  @CompoundIndex(name = "user_createdAt_idx", def = "{'userId': 1, 'createdAt': -1}"),
  @CompoundIndex(name = "user_read_idx", def = "{'userId': 1, 'read': 1}")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationModel {

  @Id private String id;

  @Indexed private String userId;

  private String title;

  private String message;

  private String category; // TRAINING, QUIZ, OFFER

  private String contentId; // trainingId / quizId / offerId

  private boolean read;

  private boolean deleted;

  @Indexed private Instant createdAt;
}
