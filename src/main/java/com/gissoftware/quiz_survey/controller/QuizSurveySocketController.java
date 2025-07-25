package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.PushQuizSurveyMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class QuizSurveySocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public QuizSurveySocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Call this method when new survey is created
    public void pushNewSurvey(String surveyId, Boolean isMandatory, List<String> targetedUsers) {
        PushQuizSurveyMessage message = new PushQuizSurveyMessage(surveyId, isMandatory, targetedUsers);
        messagingTemplate.convertAndSend("/quizSurvey", message);
    }

}

