package com.gissoftware.quiz_survey.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuizDataDTO {

  private String quizId;
  private String quizName;
  private Instant quizDate;
  private double score;
}
