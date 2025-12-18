package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TrainingEngagementDTO {
  private String userId;
  private String learner;
  private String trainingId;
  private String video;
  private int progress;
  private String status;
}
