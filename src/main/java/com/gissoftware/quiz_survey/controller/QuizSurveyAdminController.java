package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.service.QuizSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/quiz-survey")
@RequiredArgsConstructor
public class QuizSurveyAdminController {

    private final QuizSurveyService service;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<QuizSurveyModel>> createQuizSurvey(@RequestBody QuizSurveyModel model) {
        QuizSurveyModel created = service.createQuizSurvey(model);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(true,
                "Quiz & survey created successfully", created));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<QuizSurveyModel>> updateQuizSurvey(
            @PathVariable String id,
            @RequestBody QuizSurveyModel updatedModel
    ) {
        updatedModel.setId(id);
        QuizSurveyModel updated = service.updateQuizSurvey(updatedModel);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Quiz & survey updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteQuizSurvey(@PathVariable String id) {
        service.deleteQuizSurvey(id);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Quiz & survey deleted successfully", null));
    }
}
