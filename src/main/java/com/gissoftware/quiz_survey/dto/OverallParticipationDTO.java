package com.gissoftware.quiz_survey.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OverallParticipationDTO {
  private String quizSurveyId;
  private String title;
  private String type; // quiz | survey
  private String region;
  private String outlet;

  private String userId;
  private String staffId;
  private String username;

  private boolean participated;

  private Integer score;
  private Integer maxScore;
  private Double percentage;

  private String result; // PASS / FAIL / SUBMITTED / NOT_SUBMITTED

  // Question-level fields (null if survey or not participated)
  private String question;
  private String agentAnswer;
  private String correctAnswer;
  private Boolean completion;
  private Instant quizOpenTime;
  private Instant agentOpenTime;
  private Instant agentSubmissionTime;
}
