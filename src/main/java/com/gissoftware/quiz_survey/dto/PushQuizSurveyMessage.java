package com.gissoftware.quiz_survey.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PushQuizSurveyMessage {
    private String quizSurveyId;
    private Boolean isMandatory;
    private List<String> targetedUsers;
}
