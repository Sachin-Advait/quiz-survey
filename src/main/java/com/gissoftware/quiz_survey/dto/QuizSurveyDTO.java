package com.gissoftware.quiz_survey.dto;

import com.gissoftware.quiz_survey.model.SurveyDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSurveyDTO {
    private String id;
    private String type;
    private String title;
    private SurveyDefinition definitionJson;
    private Map<String, Object> answerKey;
    private Integer maxScore;
    private String quizDuration;
    private Instant createdAt;
    private Integer maxRetake;
}
