package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.UserTrainingDTO;
import com.gissoftware.quiz_survey.model.TrainingAssignment;
import com.gissoftware.quiz_survey.model.TrainingMaterial;
import com.gissoftware.quiz_survey.service.TrainingService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/training")
@RequiredArgsConstructor
public class TrainingController {

  private final TrainingService trainingService;

  // ================= ADMIN =================

  @PostMapping
  public ResponseEntity<TrainingMaterial> uploadTraining(@RequestBody TrainingMaterial material) {
    return ResponseEntity.ok(trainingService.uploadTraining(material));
  }

  @GetMapping
  public ResponseEntity<List<TrainingMaterial>> getAllTrainings() {
    return ResponseEntity.ok(trainingService.getAllMaterials());
  }

  @GetMapping("/region/{region}")
  public ResponseEntity<List<TrainingMaterial>> getByRegion(@PathVariable String region) {
    return ResponseEntity.ok(trainingService.getMaterialsByRegion(region));
  }

  @PostMapping("/assign")
  public ResponseEntity<Void> assignTraining(@RequestBody Map<String, Object> payload) {
    String trainingId = (String) payload.get("trainingId");
    List<String> userIds = (List<String>) payload.get("userIds");
    Instant dueDate = Instant.parse((String) payload.get("dueDate"));

    trainingService.assignTraining(trainingId, userIds, dueDate);
    return ResponseEntity.ok().build();
  }

  // ================= USER =================

  @GetMapping("/user/{userId}")
  public ResponseEntity<List<TrainingAssignment>> getUserTrainings(@PathVariable String userId) {
    return ResponseEntity.ok(trainingService.getUserTrainings(userId));
  }

  @GetMapping("/user/{userId}/details")
  public ResponseEntity<List<UserTrainingDTO>> getUserTrainingDetails(@PathVariable String userId) {
    return ResponseEntity.ok(trainingService.getUserTrainingDetails(userId));
  }

  @PostMapping("/progress")
  public ResponseEntity<TrainingAssignment> updateProgress(
      @RequestBody Map<String, Object> payload) {
    String userId = (String) payload.get("userId");
    String trainingId = (String) payload.get("trainingId");
    Integer progress = (Integer) payload.get("progress");

    return ResponseEntity.ok(trainingService.updateProgress(userId, trainingId, progress));
  }
}
