package com.gissoftware.quiz_survey.controller;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class QuizSurveySocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public QuizSurveySocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Call this method when new survey is created
//    public void pushNewSurvey(String surveyId, Boolean isMandatory, List<String> targetedUsers) {
//        PushQuizSurveyMessage message = new PushQuizSurveyMessage(surveyId, isMandatory, targetedUsers);
//        messagingTemplate.convertAndSend("/quizSurvey", message);
//    }

}

