package com.gissoftware.quiz_survey.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateAnnouncementRequest {
    private String quizSurveyId;
    private String message;
    private List<String> targetUser;
}

