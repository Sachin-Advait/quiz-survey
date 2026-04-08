package com.gissoftware.quiz_survey.dto;


import com.gissoftware.quiz_survey.model.QuizSurveyModel;
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
    private QuizSurveyModel data;
}
