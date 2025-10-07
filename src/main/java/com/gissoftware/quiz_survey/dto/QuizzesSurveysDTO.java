package com.gissoftware.quiz_survey.dto;

import com.gissoftware.quiz_survey.model.VisibilityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

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
    private String quizTotalDuration;
    private String quizDuration;
    private Boolean isAnnounced;
    private Boolean isParticipated;
    private Boolean isMandatory;
    private Instant createdAt;
    private Integer maxRetake;
    private List<String> userDataDisplayFields;
    private VisibilityType visibilityType;
}
