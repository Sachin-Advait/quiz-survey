package com.gissoftware.quiz_survey.dto;

import com.gissoftware.quiz_survey.model.AnnouncementMode;
import com.gissoftware.quiz_survey.model.SurveyDefinition;
import com.gissoftware.quiz_survey.model.VisibilityType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSurveyDTO {
  private String id;
  private String type;
  private String title;
  private SurveyDefinition definitionJson;
  private Map<String, Object> answerKey;
  private Integer maxScore;
  private String quizDuration;
  private String quizTotalDuration;
  private Instant createdAt;
  private Integer maxRetake;
  private VisibilityType visibilityType;
  private AnnouncementMode announcementMode;
  private List<String> userDataDisplayFields;
}
