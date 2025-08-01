package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QuizCompletionStatsDTO {
    private int totalAssigned;
    private int totalCompleted;
    private int totalNotCompleted;
    private double completionRate;
}
