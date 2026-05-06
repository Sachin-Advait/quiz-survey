package com.gissoftware.quiz_survey.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OverallParticipationDTO {
  private String quizSurveyId;
  private String title;
  private String type; // quiz | survey

  private String userId;
  private String staffId;
  private String username;

  private boolean participated;

  private Integer score; // quiz only
  private Integer maxScore; // quiz only
  private Double percentage;

  private String result; // PASS / FAIL / SUBMITTED / NOT_SUBMITTED
}
