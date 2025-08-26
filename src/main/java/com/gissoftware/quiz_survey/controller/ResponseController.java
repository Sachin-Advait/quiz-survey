package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.dto.ResponseReceivedDTO;
import com.gissoftware.quiz_survey.dto.SurveySubmissionRequest;
import com.gissoftware.quiz_survey.dto.UserResponseDTO;
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

    @PostMapping("/user/submit-survey/{id}")
    public ResponseEntity<ApiResponseDTO<ResponseModel>> submitResponse(
            @PathVariable String id,
            @RequestBody SurveySubmissionRequest request
    ) {
        ResponseModel response = responseService.storeResponse(id, request);
        return ResponseEntity.ok(new ApiResponseDTO<>(true,
                "Response submitted successfully", response));
    }

    @GetMapping("/user/responses/by-user/{userId}")
    public ResponseEntity<ApiResponseDTO<List<ResponseModel>>> getAllResponsesByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true,
                        "Response submitted successfully", responseService.getAllResponsesByUserId(userId))
        );
    }

    @GetMapping("/user/responses/staff-invited/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<List<UserResponseDTO>>> totalStaffInvited(@PathVariable String quizSurveyId) {
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true,
                        "Staff Invited retrieved successfully", responseService.totalStaffInvited(quizSurveyId))
        );
    }

    @GetMapping("/user/responses/response-received/{quizSurveyId}")
    public ResponseEntity<ApiResponseDTO<List<ResponseReceivedDTO>>> totalResponseReceived(@PathVariable String quizSurveyId) {
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true,
                        "Respondents retrieved successfully", responseService.totalResponseReceived(quizSurveyId))
        );
    }
}
