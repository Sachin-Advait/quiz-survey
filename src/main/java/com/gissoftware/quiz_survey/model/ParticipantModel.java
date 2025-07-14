package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("participants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantModel {
    @Id
    private String id;
    private String quizSurveyId;
    private String externalUserId;
    @Builder.Default
    private Instant startedAt = Instant.now();
    private Instant completedAt;
}

