package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.*;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.service.AttemptService;
import com.gissoftware.quiz_survey.service.ResponseService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@AllArgsConstructor
@RequestMapping("/api")
@RestController
public class ResponseController {

  private final ResponseService responseService;
  private final AttemptService attemptService;

  @PostMapping("/user/submit/{id}")
  public ResponseEntity<ApiResponseDTO<ResponseModel>> submitResponse(
      @PathVariable String id,
      @RequestBody SurveySubmissionRequest request,
      HttpServletRequest httpRequest) {

    String userAgent = httpRequest.getHeader("User-Agent");

    ResponseModel response = responseService.storeResponse(id, request, userAgent);

    return ResponseEntity.ok(
        new ApiResponseDTO<>(true, "Response submitted successfully", response));
  }

  @GetMapping("/user/responses/by-user/{userId}")
  public ResponseEntity<ApiResponseDTO<List<ResponseModel>>> getAllResponsesByUserId(
      @PathVariable String userId) {
    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true,
            "Response submitted successfully",
            responseService.getAllResponsesByUserId(userId)));
  }

  @GetMapping("/user/responses/staff-invited/{quizSurveyId}")
  public ResponseEntity<ApiResponseDTO<List<UserResponseDTO>>> totalStaffInvited(
      @PathVariable String quizSurveyId) {
    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true,
            "Staff Invited retrieved successfully",
            responseService.totalStaffInvited(quizSurveyId)));
  }

  @GetMapping("/user/responses/response-received/{quizSurveyId}")
  public ResponseEntity<ApiResponseDTO<List<ResponseReceivedDTO>>> totalResponseReceived(
      @PathVariable String quizSurveyId) {
    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true,
            "Respondents retrieved successfully",
            responseService.totalResponseReceived(quizSurveyId)));
  }

  @GetMapping("/user/responses/low-scorers")
  public ResponseEntity<ApiResponseDTO<List<LowScoringUserDTO>>> lowScorersLast5Weeks() {
    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true,
            "Low scorers during last 5 weeks retrieved successfully",
            responseService.getLowScoringUsers(5, 50)));
  }

  @GetMapping("/user/attempt/{quizSurveyId}")
  public ResponseEntity<ApiResponseDTO<AttemptResponseDTO>> startOrResume(
      @PathVariable String quizSurveyId, @RequestParam String userId) {
    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true, "Success", attemptService.startOrResumeAttempt(quizSurveyId, userId)));
  }

  @PutMapping("/user/attempt/{quizSurveyId}/answers")
  public ResponseEntity<ApiResponseDTO<Void>> saveAnswers(
      @PathVariable String quizSurveyId, @RequestBody SaveAnswersRequest request) {
    attemptService.saveAnswers(quizSurveyId, request);
    return ResponseEntity.ok(new ApiResponseDTO<>(true, "Answers saved", null));
  }
}
