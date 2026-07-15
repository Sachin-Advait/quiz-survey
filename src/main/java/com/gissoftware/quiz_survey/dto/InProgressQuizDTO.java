package com.gissoftware.quiz_survey.dto;

import java.time.Instant;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InProgressQuizDTO {
    private String quizSurveyId;
    private String quizTitle;
    private String quizType;
    private Instant endTime;
    private Long remainingSeconds;
    private Instant lastSavedAt;
    private Integer attemptNumber;
}