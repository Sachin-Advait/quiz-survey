package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.dto.QuizResultAdminDTO;
import com.gissoftware.quiz_survey.dto.QuizResultDTO;
import com.gissoftware.quiz_survey.dto.SurveyResultDTO;
import com.gissoftware.quiz_survey.service.ResultService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RequestMapping("/api")
@RestController
public class ResultController {

    private final ResultService resultService;

    @GetMapping("/user/quiz-result/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<QuizResultDTO>> getQuizResultByUserId(
            @PathVariable String quizSurveyId,
            @RequestParam String userId
    ) {
        QuizResultDTO response = resultService.getQuizResultByUserId(quizSurveyId, userId);

        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Retrieved submitted response successfully", response));

    }

    @GetMapping("/admin/quiz-result/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<List<QuizResultAdminDTO>>> getQuizResultsAdmin(@PathVariable String quizSurveyId) {
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Retrieved submitted responses successfully",
                        resultService.getQuizResultsAdmin(quizSurveyId))
        );
    }

    @GetMapping("/user/all-survey-result/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<List<SurveyResultDTO>>> getSurveyResultsAdmin(
            @PathVariable String quizSurveyId,
            @RequestParam(required = false) String userId) {
        List<SurveyResultDTO> results;

        try {
            results = resultService.getSurveyResultsAdmin(quizSurveyId, userId);
        } catch (IllegalArgumentException ex) {
            ApiResponseDTO<List<SurveyResultDTO>> emptyResponse = new ApiResponseDTO<>(true,
                    "No survey responses found.", List.of());
            return ResponseEntity.ok(emptyResponse);
        }
        ApiResponseDTO<List<SurveyResultDTO>> response = new ApiResponseDTO<>(
                true,
                "Surveys result retrieved successfully",
                results
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/survey-result/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<List<SurveyResultDTO>>> getSurveyResultByUserId(
            @PathVariable String quizSurveyId,
            @RequestParam String userId) {
        List<SurveyResultDTO> results;

        try {
            results = resultService.getSurveyResultsByUserId(quizSurveyId, userId);
        } catch (IllegalArgumentException ex) {
            ApiResponseDTO<List<SurveyResultDTO>> emptyResponse = new ApiResponseDTO<>(true,
                    "No survey responses found.", List.of());
            return ResponseEntity.ok(emptyResponse);
        }
        ApiResponseDTO<List<SurveyResultDTO>> response = new ApiResponseDTO<>(
                true,
                "Surveys result retrieved successfully",
                results
        );
        return ResponseEntity.ok(response);
    }
}
