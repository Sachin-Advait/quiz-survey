package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.TrainingEditDTO;
import com.gissoftware.quiz_survey.dto.TrainingEngagementDTO;
import com.gissoftware.quiz_survey.dto.TrainingUploadAssignDTO;
import com.gissoftware.quiz_survey.dto.UserTrainingDTO;
import com.gissoftware.quiz_survey.mapper.TrainingMapper;
import com.gissoftware.quiz_survey.model.TrainingAssignment;
import com.gissoftware.quiz_survey.model.TrainingMaterial;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.TrainingAssignmentRepository;
import com.gissoftware.quiz_survey.repository.TrainingMaterialRepository;
import com.gissoftware.quiz_survey.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrainingService {

  private final TrainingMaterialRepository materialRepo;
  private final TrainingAssignmentRepository assignmentRepo;
  private final UserRepository userRepo;
  private final FCMService fcmService;

  // ================= ADMIN =================

  public TrainingMaterial uploadAndAssign(TrainingUploadAssignDTO request) {

    TrainingMaterial material = request.getMaterial();
    material.setAssignedTo(0);
    material.setCompletionRate(0);
    material.setViews(0);
    material.setUploadDate(Instant.now());

    TrainingMaterial savedMaterial = materialRepo.save(material);

    List<String> newlyAssignedUsers = new ArrayList<>();

    if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
      for (String userId : request.getUserIds()) {

        boolean alreadyAssigned =
            assignmentRepo.findByUserIdAndTrainingId(userId, savedMaterial.getId()).isPresent();

        if (alreadyAssigned) continue;

        TrainingAssignment assignment =
            TrainingAssignment.builder()
                .userId(userId)
                .trainingId(savedMaterial.getId())
                .progress(0)
                .status("not-started")
                .dueDate(request.getDueDate())
                .assignedAt(Instant.now())
                .build();

        assignmentRepo.save(assignment);
        newlyAssignedUsers.add(userId);
      }

      savedMaterial.setAssignedTo((int) assignmentRepo.countByTrainingId(savedMaterial.getId()));
      materialRepo.save(savedMaterial);
    }

    // 🔔 Notify ONLY newly assigned users
    if (!newlyAssignedUsers.isEmpty()) {
      fcmService.notifyTrainingAssigned(savedMaterial.getId(), newlyAssignedUsers);
    }

    return savedMaterial;
  }

  public List<TrainingMaterial> getAllMaterials() {
    return materialRepo.findAllByActiveTrue();
  }

  public List<TrainingMaterial> getMaterialsByRegion(String region) {
    if ("all".equalsIgnoreCase(region)) {
      return materialRepo.findAllByActiveTrue();
    }
    return materialRepo.findByRegionAndActiveTrue(region);
  }

  public void assignTraining(String trainingId, List<String> userIds, Instant dueDate) {
    for (String userId : userIds) {
      boolean alreadyAssigned =
          assignmentRepo.findByUserIdAndTrainingId(userId, trainingId).isPresent();
      if (alreadyAssigned) continue;

      TrainingAssignment assignment =
          TrainingAssignment.builder()
              .userId(userId)
              .trainingId(trainingId)
              .progress(0)
              .status("not-started")
              .dueDate(dueDate)
              .assignedAt(Instant.now())
              .build();

      assignmentRepo.save(assignment);
    }

    long totalAssigned = assignmentRepo.countByTrainingId(trainingId);
    materialRepo
        .findById(trainingId)
        .ifPresent(
            material -> {
              material.setAssignedTo((int) totalAssigned);
              materialRepo.save(material);
            });
    fcmService.notifyTrainingAssigned(trainingId, userIds);
  }

  // ================= USER =================
  public List<TrainingAssignment> getUserTrainings(String userId) {
    return assignmentRepo.findByUserId(userId);
  }

  public List<UserTrainingDTO> getUserTrainingDetails(String userId) {

    List<TrainingAssignment> assignments = assignmentRepo.findByUserId(userId);

    return assignments.stream()
        .map(
            assignment -> {
              TrainingMaterial material =
                  materialRepo
                      .findByIdAndActiveTrue(assignment.getTrainingId())
                      .orElse(null); // 👈 IMPORTANT

              // 🚫 Skip deleted trainings
              if (material == null) return null;

              return new UserTrainingDTO(
                  assignment.getId(),
                  material.getId(),
                  material.getTitle(),
                  material.getType(),
                  material.getDuration(),
                  material.getCloudinaryUrl(),
                  material.getCloudinaryFormat(),
                  material.getCloudinaryResourceType(),
                  assignment.getProgress(),
                  assignment.getStatus(),
                  assignment.getDueDate());
            })
        .filter(Objects::nonNull) // 👈 removes deleted ones
        .toList();
  }

  public TrainingAssignment updateProgress(String userId, String trainingId, int progress) {
    TrainingAssignment assignment =
        assignmentRepo
            .findByUserIdAndTrainingId(userId, trainingId)
            .orElseThrow(() -> new RuntimeException("Training not assigned"));

    int current = assignment.getProgress();

    if (progress > current) assignment.setProgress(progress);

    if (assignment.getProgress() >= 100) {
      assignment.setStatus("completed");
    } else if (progress > 0) {
      assignment.setStatus("in-progress");
    }
    if (assignment.getProgress() >= 100) {
      long totalAssigned = assignmentRepo.countByTrainingId(trainingId);
      long completed = assignmentRepo.countByTrainingIdAndStatus(trainingId, "completed");

      materialRepo
          .findById(trainingId)
          .ifPresent(
              material -> {
                int rate = totalAssigned == 0 ? 0 : (int) ((completed * 100) / totalAssigned);
                material.setCompletionRate(rate);
                materialRepo.save(material);
              });
    }

    return assignmentRepo.save(assignment);
  }

  public List<TrainingEngagementDTO> getEngagement(String trainingId) {
    List<TrainingAssignment> assignments;
    if (trainingId != null) assignments = assignmentRepo.findByTrainingId(trainingId);
    else assignments = assignmentRepo.findAll();

    return assignments.stream()
        .map(
            a -> {
              UserModel user = userRepo.findById(a.getUserId()).orElse(null);

              TrainingMaterial material =
                  materialRepo.findByIdAndActiveTrue(a.getTrainingId()).orElse(null);

              if (user == null || material == null) return null;

              return TrainingEngagementDTO.builder()
                  .userId(user.getId())
                  .learner(user.getUsername())
                  .trainingId(material.getId())
                  .video(material.getTitle())
                  .progress(a.getProgress())
                  .status(a.getStatus())
                  .build();
            })
        .filter(Objects::nonNull)
        .toList();
  }

  public TrainingMaterial updateTraining(String trainingId, TrainingUploadAssignDTO request) {

    TrainingMaterial material =
        materialRepo
            .findById(trainingId)
            .orElseThrow(() -> new RuntimeException("Training not found"));

    /* =========================
    1️⃣ UPDATE TRAINING META
    ========================= */

    // ✅ Update basic fields (flat payload support)
    if (request.getMaterial().getTitle() != null) {
      material.setTitle(request.getMaterial().getTitle());
    }

    if (request.getMaterial().getRegion() != null) {
      material.setRegion(request.getMaterial().getRegion().toLowerCase());
    }

    if (request.getMaterial().getType() != null) {
      material.setType(request.getMaterial().getType());
    }

    if (request.getMaterial().getDuration() != null) {
      material.setDuration(request.getMaterial().getDuration());
    }

    // ✅ Update Cloudinary fields ONLY if sent
    if (request.getMaterial().getCloudinaryUrl() != null) {
      material.setCloudinaryUrl(request.getMaterial().getCloudinaryUrl());
      material.setCloudinaryPublicId(request.getMaterial().getCloudinaryPublicId());
      material.setCloudinaryResourceType(request.getMaterial().getCloudinaryResourceType());
      material.setCloudinaryFormat(request.getMaterial().getCloudinaryFormat());
    }

    materialRepo.save(material);

    /* =========================
    2️⃣ ASSIGNMENT MANAGEMENT
    ========================= */

    List<String> newUserIds = request.getUserIds() != null ? request.getUserIds() : List.of();

    List<TrainingAssignment> existingAssignments = assignmentRepo.findByTrainingId(trainingId);

    List<String> existingUserIds =
        existingAssignments.stream().map(TrainingAssignment::getUserId).toList();

    List<String> newlyAssignedUsers = new ArrayList<>();

    // ➕ ADD new assignments
    for (String userId : newUserIds) {
      if (existingUserIds.contains(userId)) continue;

      TrainingAssignment assignment =
          TrainingAssignment.builder()
              .trainingId(trainingId)
              .userId(userId)
              .progress(0)
              .status("not-started")
              .dueDate(request.getDueDate())
              .assignedAt(Instant.now())
              .build();

      assignmentRepo.save(assignment);
      newlyAssignedUsers.add(userId);
    }

    // 🔁 UPDATE due date OR ❌ REMOVE unselected users
    for (TrainingAssignment assignment : existingAssignments) {
      if (newUserIds.contains(assignment.getUserId())) {
        assignment.setDueDate(request.getDueDate());
        assignmentRepo.save(assignment);
      } else {
        assignmentRepo.delete(assignment);
      }
    }

    /* =========================
    3️⃣ UPDATE COUNTS & NOTIFY
    ========================= */

    material.setAssignedTo((int) assignmentRepo.countByTrainingId(trainingId));
    materialRepo.save(material);

    if (!newlyAssignedUsers.isEmpty()) {
      fcmService.notifyTrainingAssigned(trainingId, newlyAssignedUsers);
    }

    return material;
  }

  public void deleteTraining(String trainingId) {

    TrainingMaterial material =
        materialRepo
            .findById(trainingId)
            .orElseThrow(() -> new RuntimeException("Training not found"));

    // 🔒 SOFT DELETE
    material.setActive(false);
    material.setDeletedAt(Instant.now());

    materialRepo.save(material);
  }

  public TrainingEditDTO getTrainingById(String trainingId) {

    TrainingMaterial material =
        materialRepo
            .findByIdAndActiveTrue(trainingId)
            .orElseThrow(() -> new RuntimeException("Training not found"));

    List<TrainingAssignment> assignments = assignmentRepo.findByTrainingId(trainingId);

    return TrainingMapper.toEditDTO(material, assignments);
  }
}
