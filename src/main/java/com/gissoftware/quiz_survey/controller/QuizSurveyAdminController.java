package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.*;
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

    private final QuizSurveyService quizSurveyService;

    @PostMapping
    public ResponseEntity<ApiResponseDTO<QuizSurveyModel>> createQuizSurvey(@RequestBody QuizSurveyModel model) {
        QuizSurveyModel created = quizSurveyService.createQuizSurvey(model);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(true,
                "Quiz & survey created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<QuizSurveyModel>> updateQuizSurvey(
            @PathVariable String id,
            @RequestBody QuizSurveyModel updatedModel
    ) {
        updatedModel.setId(id);
        QuizSurveyModel updated = quizSurveyService.updateQuizSurvey(updatedModel);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Quiz & survey updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteQuizSurvey(@PathVariable String id) {
        quizSurveyService.deleteQuizSurvey(id);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Quiz & survey deleted successfully", null));
    }

    @GetMapping("/quiz-insights/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<QuizInsightsDTO>> getQuizInsights(@PathVariable String quizSurveyId) {
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Quiz insights retrieved successfully", quizSurveyService.getQuizInsights(quizSurveyId)));
    }

    @GetMapping("/completion-stats/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<QuizCompletionStatsDTO>> getQuizStats(@PathVariable String quizSurveyId) {
        QuizCompletionStatsDTO stats = quizSurveyService.getQuizCompletionStats(quizSurveyId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Quiz completion stats retrieved successfully", stats));
    }

    @GetMapping("/survey-insights/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<SurveyResponseStatsDTO>> getSurveyInsights(@PathVariable String quizSurveyId) {
        SurveyResponseStatsDTO insights = quizSurveyService.getSurveyResponseStats(quizSurveyId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Survey insights retrieved successfully", insights));
    }

    @GetMapping("/survey-activity-stats/{id}")
    public ResponseEntity<ApiResponseDTO<SurveyActivityStatsDTO>> getSurveyActivityStats(@PathVariable String id) {
        SurveyActivityStatsDTO stats = quizSurveyService.getSurveyActivityStats(id);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Survey stats retrieved successfully", stats));
    }

    @GetMapping("/satisfaction-insights/{surveyId}")
    public ResponseEntity<ApiResponseDTO<SatisfactionInsightResponse>> getSatisfactionInsights(@PathVariable String surveyId) {

        SatisfactionInsightResponse satisfactionInsightResponse = quizSurveyService.getSatisfactionInsights(surveyId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Survey satisfaction insights retrieved successfully", satisfactionInsightResponse));
    }
}
