package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuizScoreSummaryDTO {
    private int totalAttempts;
    private double averageScore;
    private int highestScore;
    private int maxScore;
}
