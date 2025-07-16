package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class SurveyResultDTO {
    private String question;
    private String type; // "choice" or "rating"
    private Map<String, Object> result;
}
