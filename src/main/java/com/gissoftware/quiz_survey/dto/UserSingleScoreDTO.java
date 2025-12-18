package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSingleScoreDTO {

  private String userId;

  private double quizScore; // 0–100
  private double surveyParticipation; // 0–100
  private double satisfactionScore; // 0–100

  private double finalScore; // weighted final score
}
