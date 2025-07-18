package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.dto.QuizScoreSummaryDTO;
import com.gissoftware.quiz_survey.dto.SurveyResultDTO;
import com.gissoftware.quiz_survey.dto.SurveySubmissionDTO;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.service.ResponseService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@AllArgsConstructor
@RequestMapping("/api")
@RestController
public class ResponseController {

    private final ResponseService responseService;

    @PostMapping("/submit-survey/{id}")
    public ResponseEntity<ApiResponseDTO<ResponseModel>> submitResponse(
            @PathVariable String id,
            @RequestBody SurveySubmissionDTO request
    ) {
        ResponseModel response = responseService.storeResponse(id, request);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Response submitted successfully", response));
    }


    @GetMapping("/quiz-summary/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<QuizScoreSummaryDTO>> getSummary(@PathVariable String quizSurveyId) {

        QuizScoreSummaryDTO response = responseService.getScoreSummary(quizSurveyId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Response submitted successfully", response));
    }

    @GetMapping("/responses/by-user/{userId}")
    public ResponseEntity<ApiResponseDTO<List<ResponseModel>>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true,
                        "Response submitted successfully", responseService.getResponsesByUserId(userId))
        );
    }

    @GetMapping("/quiz-result/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<ResponseModel>> getResponseByUserAndQuiz(
            @PathVariable String quizSurveyId,
            @RequestParam String userId
    ) {
        ResponseModel response = responseService.getResponseByUserAndQuiz(quizSurveyId, userId);

        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Retrieved submitted response successfully", response));

    }

    @GetMapping("admin/quiz-result/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<List<ResponseModel>>> getByQuiz(@PathVariable String quizSurveyId) {
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Retrieved submitted responses successfully",
                        responseService.getResponsesByQuizSurveyId(quizSurveyId))
        );
    }

    @GetMapping("/survey-results/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<List<SurveyResultDTO>>> getSurveyResults(@PathVariable String quizSurveyId) {
        List<SurveyResultDTO> results;

        try {
            results = responseService.getSurveyResults(quizSurveyId);
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
