package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document("quizsurvey")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSurveyModel {

    @Id
    private String id;
    private String type;
    private String title;
    private SurveyDefinition definitionJson;
    private Map<String, Object> answerKey; // Map of questionId -> correctAnswer
    private Integer maxScore;
    private Boolean status;
    private String quizTotalDuration;
    @Builder.Default
    private Instant createdAt = Instant.now();
}
