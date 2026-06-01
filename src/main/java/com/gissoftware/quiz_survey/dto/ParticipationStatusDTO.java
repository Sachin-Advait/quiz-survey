package com.gissoftware.quiz_survey.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParticipationStatusDTO {
  private String userId;
  private String staffId;
  private String username;
  private String region;
  private String outlet;
  private String position;

  private String title;

  private boolean participated;

  private Integer score;
  private Integer maxScore;
  private Double percentage;
  private Integer marks;
  private String result;

  // Question-level fields
  private String question;
  private String agentAnswer;
  private String correctAnswer;
  private Boolean completion;
  private Instant quizOpenTime;
  private Instant agentOpenTime;
  private Instant agentSubmissionTime;
}
