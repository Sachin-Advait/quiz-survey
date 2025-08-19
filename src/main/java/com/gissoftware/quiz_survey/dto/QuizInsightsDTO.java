package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
public class QuizInsightsDTO {
    private String title;
    private double averageScore;
    private double passRate;
    private double failRate;
    private ScorerDTO topScorer;
    private ScorerDTO lowestScorer;
    private List<MostIncorrectQuestionDTO> mostIncorrectQuestions;

    @Data
    @AllArgsConstructor
    public static class MostIncorrectQuestionDTO {
        private String question;
        private int wrongAttempts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScorerDTO {
        private String name;
        private Integer score;
    }

}
