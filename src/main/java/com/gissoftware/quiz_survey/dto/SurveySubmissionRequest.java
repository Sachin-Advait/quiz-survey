package com.gissoftware.quiz_survey.dto;


import lombok.Data;

import java.util.Map;

@Data
public class SurveySubmissionRequest {
    private String userId;
    private Map<String, Object> answers;
}
