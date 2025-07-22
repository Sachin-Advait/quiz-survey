package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document("quizsurvey")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EnableMongoAuditing
public class QuizSurveyModel {

    @Id
    private String id;
    private String type;
    private String title;
    private SurveyDefinition definitionJson;
    private Map<String, Object> answerKey;
    private Integer maxScore;
    private Boolean status;
    private Duration quizTotalDuration;
    private Duration quizDuration;
    private Boolean isAnnounced;
    private Boolean isMandatory;
    @Builder.Default
    private List<String> targetedUsers = new ArrayList<>();
    @CreatedDate
    private Instant createdAt;
}
