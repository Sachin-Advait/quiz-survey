package com.gissoftware.quiz_survey.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("training_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EnableMongoAuditing
public class TrainingAssignment {

  @Id private String id;

  private String userId;
  private String trainingId;

  private Integer progress; // %
  private String status; // not-started, in-progress, completed

  private Instant dueDate;

  @CreatedDate private Instant assignedAt;
}
