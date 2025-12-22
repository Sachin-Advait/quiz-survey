package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.dto.TrainingEngagementDTO;
import com.gissoftware.quiz_survey.dto.UserTrainingDTO;
import com.gissoftware.quiz_survey.model.TrainingAssignment;
import com.gissoftware.quiz_survey.model.TrainingMaterial;
import com.gissoftware.quiz_survey.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    // ================= ADMIN =================
    @PostMapping
    public ResponseEntity<ApiResponseDTO<TrainingMaterial>> uploadTraining(@RequestBody TrainingMaterial material) {
        TrainingMaterial created = trainingService.uploadTraining(material);
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Training uploaded successfully", created)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<TrainingMaterial>>> getAllTrainings() {
        List<TrainingMaterial> materials = trainingService.getAllMaterials();
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "All trainings fetched successfully", materials)
        );
    }

    @GetMapping("/region/{region}")
    public ResponseEntity<ApiResponseDTO<List<TrainingMaterial>>> getByRegion(@PathVariable String region) {
        List<TrainingMaterial> materials = trainingService.getMaterialsByRegion(region);
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Trainings fetched for region", materials)
        );
    }

    @PostMapping("/assign")
    public ResponseEntity<ApiResponseDTO<Void>> assignTraining(@RequestBody Map<String, Object> payload) {

        String trainingId = (String) payload.get("trainingId");
        List<String> userIds = (List<String>) payload.get("userIds");
        Instant dueDate = Instant.parse((String) payload.get("dueDate"));
        trainingService.assignTraining(trainingId, userIds, dueDate);

        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Training assigned successfully", null)
        );
    }

    // ================= USER =================
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponseDTO<List<TrainingAssignment>>> getUserTrainings(@PathVariable String userId) {
        List<TrainingAssignment> assignments = trainingService.getUserTrainings(userId);

        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "User trainings fetched successfully", assignments)
        );
    }

    @GetMapping("/user/{userId}/details")
    public ResponseEntity<ApiResponseDTO<List<UserTrainingDTO>>> getUserTrainingDetails(@PathVariable String userId) {
        List<UserTrainingDTO> details = trainingService.getUserTrainingDetails(userId);
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "User training details fetched successfully", details)
        );
    }

    @PostMapping("/progress")
    public ResponseEntity<ApiResponseDTO<TrainingAssignment>> updateProgress(@RequestBody Map<String, Object> payload) {

        String userId = (String) payload.get("userId");
        String trainingId = (String) payload.get("trainingId");
        Integer progress = (Integer) payload.get("progress");

        TrainingAssignment updated = trainingService.updateProgress(userId, trainingId, progress);
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Training progress updated successfully", updated)
        );
    }

    @GetMapping("/engagement")
    public ResponseEntity<ApiResponseDTO<List<TrainingEngagementDTO>>> getEngagement(
            @RequestParam(required = false) String trainingId) {
        List<TrainingEngagementDTO> engagement = trainingService.getEngagement(trainingId);
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Engagement fetched successfully", engagement)
        );
    }
}
