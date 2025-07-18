package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document("responses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseModel {
    @Id
    private String id;
    private String quizSurveyId;
    private String userId;
    private String username;
    private Map<String, Object> answers;
    private Integer score; // null for survey
    private Integer maxScore; // null for survey
    @Builder.Default
    private Instant submittedAt = Instant.now();
}
