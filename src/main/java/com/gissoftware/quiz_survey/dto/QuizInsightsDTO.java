package com.gissoftware.quiz_survey.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuizInsightsDTO {
    private double averageScore;
    private double passRate;
    private double failRate;
    private String topScorer;
    private Integer topScore;
    private String lowScorer;
    private Integer lowScore;
    private List<String> mostIncorrectQuestions;
}
