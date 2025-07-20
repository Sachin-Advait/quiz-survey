package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.dto.QuizSurveyDTO;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.service.QuizSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz-survey")
@RequiredArgsConstructor
public class QuizSurveyController {

    private final QuizSurveyService service;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<QuizSurveyModel>> getQuizSurvey(@PathVariable String id) {
        QuizSurveyModel quizSurvey = service.getQuizSurvey(id);
        if (quizSurvey == null) {
            return ResponseEntity.status(404).body(
                    new ApiResponseDTO<>(false, "Quiz & survey not found", null)
            );
        }
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Quiz & survey retrieved successfully", quizSurvey));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<QuizSurveyDTO>>> getQuizzesSurveys(
            @RequestParam(required = false) String userId
    ) {
        List<QuizSurveyDTO> surveys = service.getQuizzesSurveys(userId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "All quiz & surveys retrieved successfully", surveys));
    }
}
