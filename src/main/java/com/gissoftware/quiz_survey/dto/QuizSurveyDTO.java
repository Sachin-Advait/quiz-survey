package com.gissoftware.quiz_survey.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.DurationDeserializer;
import com.gissoftware.quiz_survey.model.SurveyDefinition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
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

    @JsonSerialize(using = ToStringSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration quizDuration;

    private Instant createdAt;
}
