package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizzesSurveysDTO {
    private String id;
    private String type;
    private String title;
    private Integer totalQuestion;
    private Boolean status;

    //    @JsonSerialize(using = ToStringSerializer.class)
//    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration quizTotalDuration;
    private Boolean isAnnounced;
    private Boolean isParticipated;
    private Boolean isMandatory;
    private Instant createdAt;
}
