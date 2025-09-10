package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "questions")
public class QuestionModel {
    @Id
    private String id;

    private String text;
    private List<String> options;  // multiple-choice options
    private String answer;         // correct answer
    private Instant createdAt = Instant.now();
}
