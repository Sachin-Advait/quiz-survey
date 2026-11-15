package com.gissoftware.quiz_survey.model;

import java.time.Instant;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Snapshot of a quiz/survey's questions before it was edited in place. */
@Document("quizsurvey_versions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSurveyVersionModel {
  @Id private String id;
  private String quizSurveyId;
  private Integer version;
  private String title;
  private SurveyDefinition definitionJson;
  private Map<String, Object> answerKey;
  private Integer maxScore;
  private String completedUsersPolicy; // policy applied when this version was replaced
  private Instant replacedAt;
}
