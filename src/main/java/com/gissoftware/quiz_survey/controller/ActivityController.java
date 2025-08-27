package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.dto.UserResponseDTO;
import com.gissoftware.quiz_survey.service.ActivityService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user/survey-activity")
@AllArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    // Get the least active users by region
    @GetMapping("/least-active-users/region")
    public ResponseEntity<ApiResponseDTO<List<UserResponseDTO>>> getUsersByRegion(
            @RequestParam String surveyId,
            @RequestParam String region
    ) {
        List<UserResponseDTO> users = activityService.getNonRespondedUsersByRegion(surveyId, region);
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Least active user in region", users)
        );
    }

    // Get the least active users by outlet
    @GetMapping("/least-active-users/outlet")
    public ResponseEntity<ApiResponseDTO<List<UserResponseDTO>>> getUsersByOutlet(
            @RequestParam String surveyId,
            @RequestParam String outlet
    ) {
        List<UserResponseDTO> users = activityService.getNonRespondedUsersByOutlet(surveyId, outlet);
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Least active user in outlet", users)
        );
    }
}
