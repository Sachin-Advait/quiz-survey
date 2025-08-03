package com.gissoftware.quiz_survey.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class SatisfactionInsightResponse {
    private String title;
    private Map<String, Double> averageSatisfactionBySurveyType;
    private List<QuestionDistribution> scoreDistributionPerQuestion;


    @Data
    @Builder
    public static class QuestionDistribution {
        private String question;
        private Map<Integer, Integer> distribution;
    }

}

