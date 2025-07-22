package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class QuizScoreSummaryDTO {
    private int totalAttempts;
    private double averageScore;
    private int highestScore;
    private int maxScore;
    private List<Map<String, Object>> topScorers;
}
