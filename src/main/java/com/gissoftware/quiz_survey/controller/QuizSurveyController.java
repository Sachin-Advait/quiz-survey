package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.*;
import com.gissoftware.quiz_survey.service.QuizSurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/user/quiz-survey")
@RequiredArgsConstructor
public class QuizSurveyController {

    private final QuizSurveyService quizSurveyService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<QuizSurveyDTO>> getQuizSurvey(@PathVariable String id) {
        QuizSurveyDTO quizSurvey = quizSurveyService.getQuizSurvey(id);
        if (quizSurvey == null) {
            return ResponseEntity.status(404).body(
                    new ApiResponseDTO<>(false, "Quiz & survey not found", null)
            );
        }
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Quiz & survey retrieved successfully", quizSurvey));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<PageResponseDTO<QuizzesSurveysDTO>>> getQuizzesSurveys(
            @RequestParam(required = false) String userId,
            @RequestParam(defaultValue = "All Status") String status,
            @RequestParam(defaultValue = "All Types") String type,
            @RequestParam(defaultValue = "Latest") String sort,
            @RequestParam(defaultValue = "All") String participation,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponseDTO<QuizzesSurveysDTO> surveys = quizSurveyService.getQuizzesSurveys(userId, status, type, sort, participation, startDate, page, size);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "All quiz & surveys retrieved successfully", surveys));
    }

    @GetMapping("/summary/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<QuizScoreSummaryDTO>> getSummary(@PathVariable String quizSurveyId) {

        QuizScoreSummaryDTO response = quizSurveyService.quizScoreSummary(quizSurveyId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Response submitted successfully", response));
    }
}
