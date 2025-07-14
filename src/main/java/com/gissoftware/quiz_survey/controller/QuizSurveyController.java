package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.service.QuizSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/quiz-survey")
@RequiredArgsConstructor
public class QuizSurveyController {

    private final QuizSurveyService service;

    @GetMapping("/{id}")
    public ResponseEntity<QuizSurveyModel> getQuizSurvey(@PathVariable String id) {
        return ResponseEntity.ok(service.getQuizSurvey(id));
    }

    @PostMapping("/{id}/responses")
    public ResponseEntity<ResponseModel> submitResponse(
            @PathVariable String id,
            @RequestHeader("X-User") String externalUserId,
            @RequestBody Map<String, Object> answers
    ) {
        return ResponseEntity.ok(service.storeResponse(id, externalUserId, answers));
    }
}
