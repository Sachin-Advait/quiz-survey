package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.service.QuizSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz-survey")
@RequiredArgsConstructor
public class QuizSurveyController {

    private final QuizSurveyService service;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<QuizSurveyModel>> getQuizSurvey(@PathVariable String id) {
        QuizSurveyModel quizSurvey = service.getQuizSurvey(id);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Quiz & survey retrieved successfully", quizSurvey));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<QuizSurveyModel>>> getQuizzesSurveys() {
        List<QuizSurveyModel> surveys = service.getQuizzesSurveys();
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "All quiz & surveys retrieved successfully", surveys));
    }

    @PostMapping("/{id}/responses")
    public ResponseEntity<ApiResponseDTO<ResponseModel>> submitResponse(
            @PathVariable String id,
            @RequestHeader("X-User") String externalUserId,
            @RequestBody Map<String, Object> answers
    ) {
        ResponseModel response = service.storeResponse(id, externalUserId, answers);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Response submitted successfully", response));
    }
}
