package com.gissoftware.quiz_survey.dto;

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

  private boolean participated;

  private Integer score; // only for quiz
  private Integer maxScore; // only for quiz
  private Double percentage;

  private String result; // PASS / FAIL / SUBMITTED / NOT_SUBMITTED
}
