package com.gissoftware.quiz_survey.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("training_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EnableMongoAuditing
@org.springframework.data.mongodb.core.index.CompoundIndex(
        name = "user_training_unique",
        def = "{ 'userId': 1, 'trainingId': 1 }",
        unique = true
)
public class TrainingAssignment {

    @Id
    private String id;

    @org.springframework.data.mongodb.core.index.Indexed
    private String userId;

    @org.springframework.data.mongodb.core.index.Indexed
    private String trainingId;

    private Integer progress;

    @org.springframework.data.mongodb.core.index.Indexed
    private String status;

    private Instant dueDate;

    @CreatedDate
    private Instant assignedAt;
}
