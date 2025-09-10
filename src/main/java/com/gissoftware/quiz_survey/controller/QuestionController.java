package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.model.QuestionModel;
import com.gissoftware.quiz_survey.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    // Import bulk questions
    @PostMapping("/import")
    public ResponseEntity<List<QuestionModel>> importQuestions(@RequestBody List<QuestionModel> questions) {
        return ResponseEntity.ok(questionService.importQuestions(questions));
    }
}
