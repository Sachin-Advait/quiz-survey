package com.gissoftware.quiz_survey.dto;

import lombok.Data;

@Data
public class AnnouncementRequest {
    private String quizSurveyId;
    private String message;
}
