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
        ResponseModel response = responseService.storeResponse(id, request.getUserId(), request.getAnswers());
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Response submitted successfully", response));
    }

    @GetMapping("/summary/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<QuizScoreSummaryDTO>> getSummary(@PathVariable String quizSurveyId) {

        QuizScoreSummaryDTO response = responseService.getScoreSummary(quizSurveyId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Response submitted successfully", response));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<ResponseModel>> getByUser(@PathVariable String userId) {
        return ResponseEntity.ok(responseService.getResponsesByUserId(userId));
    }

    @GetMapping("/by-quiz/{quizSurveyId}")
    public ResponseEntity<List<ResponseModel>> getByQuiz(@PathVariable String quizSurveyId) {
        return ResponseEntity.ok(responseService.getResponsesByQuizSurveyId(quizSurveyId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseModel> getById(@PathVariable String id) {
        return ResponseEntity.ok(responseService.getResponseById(id));
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
