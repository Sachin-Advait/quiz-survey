package com.gissoftware.quiz_survey.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document("quiz_attempts")
@CompoundIndex(name = "quiz_user_unique", def = "{'quizSurveyId': 1, 'userId': 1}")
@CompoundIndex(name = "status_endtime", def = "{'status': 1, 'endTime': 1}")
@CompoundIndex(name = "user_status", def = "{'userId': 1, 'status': 1}")
public class QuizAttempt {

  @Id private String id;

  @Indexed private String quizSurveyId;

  @Indexed private String userId;

  private String username; // Denormalized for convenience

  // Timer - Server is the single source of truth
  private Instant startTime; // When user started this attempt
  private Instant endTime; // startTime + quizDuration (computed once)
  private Instant validityEndTime; // createdAt + quizTotalDuration (when quiz expires)
  private Integer durationMinutes; // Snapshot of quizDuration at start
  private Integer totalValidityDays; // Snapshot of quizTotalDuration at start

  // Mutable partial answers - updated as user works
  private Map<String, Object> answers;

  // Progress tracking
  private Integer currentPageNo;

  @LastModifiedDate private Instant lastSavedAt;

  // Status
  @Builder.Default private AttemptStatus status = AttemptStatus.IN_PROGRESS;

  private Integer attemptNumber; // Which attempt for retakes

  // Metadata
  private String platform;
  private String client;

  @CreatedDate private Instant createdAt;

  @LastModifiedDate private Instant updatedAt;

  // Helper methods
  public boolean isExpired() {
    return Instant.now().isAfter(endTime);
  }

  public boolean isValidityExpired() {
    return Instant.now().isAfter(validityEndTime);
  }

  public long getRemainingSeconds() {
    return Math.max(0, ChronoUnit.SECONDS.between(Instant.now(), endTime));
  }

  public long getValidityRemainingSeconds() {
    return Math.max(0, ChronoUnit.SECONDS.between(Instant.now(), validityEndTime));
  }

  public boolean isInProgress() {
    return status == AttemptStatus.IN_PROGRESS;
  }

  public boolean isCompleted() {
    return status == AttemptStatus.SUBMITTED || status == AttemptStatus.AUTO_SUBMITTED;
  }
}
