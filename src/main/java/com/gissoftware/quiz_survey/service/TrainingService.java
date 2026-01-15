package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.TrainingEditDTO;
import com.gissoftware.quiz_survey.dto.TrainingEngagementDTO;
import com.gissoftware.quiz_survey.dto.TrainingUploadAssignDTO;
import com.gissoftware.quiz_survey.dto.UserTrainingDTO;
import com.gissoftware.quiz_survey.mapper.TrainingMapper;
import com.gissoftware.quiz_survey.model.TrainingAssignment;
import com.gissoftware.quiz_survey.model.TrainingMaterial;
import com.gissoftware.quiz_survey.repository.TrainingAssignmentRepository;
import com.gissoftware.quiz_survey.repository.TrainingMaterialRepository;
import com.gissoftware.quiz_survey.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrainingService {

  private final TrainingMaterialRepository materialRepo;
  private final TrainingAssignmentRepository assignmentRepo;
  private final UserRepository userRepo;
  private final FCMService fcmService;

  // ================= ADMIN =================

  @Async
  public void uploadAndAssignAsync(TrainingUploadAssignDTO request) {
    uploadAndAssign(request);
  }

  public TrainingMaterial uploadAndAssign(TrainingUploadAssignDTO request) {

    TrainingMaterial material =
        request.getMaterial() != null ? request.getMaterial() : new TrainingMaterial();

    material.setAssignedTo(0);
    material.setCompletionRate(0);
    material.setViews(0);
    material.setUploadDate(Instant.now());
    material.setActive(true);
    material.setDueDate(request.getDueDate());

    String type = material.getType(); // video | document

    // ================= VIDEO =================
    if ("video".equalsIgnoreCase(type)) {

      material.setVideoProvider("bunny");
      material.setVideoPublicId(request.getVideoPublicId());
      material.setVideoPlaybackUrl(request.getVideoPlaybackUrl());
      material.setVideoFormat(request.getVideoFormat());

      // 🔥 clear document fields
      material.setDocumentUrl(null);
      material.setDuration(material.getDuration());
      material.setDurationSeconds(material.getDurationSeconds());

    }
    // ================= DOCUMENT =================
    else if ("document".equalsIgnoreCase(type)) {

      material.setDocumentUrl(request.getDocumentUrl());

      // 🔥 clear video fields
      material.setVideoProvider(null);
      material.setVideoPublicId(null);
      material.setVideoPlaybackUrl(null);
      material.setVideoFormat(null);
      material.setDuration(null);
    }

    TrainingMaterial savedMaterial = materialRepo.save(material);

    // ================= ASSIGN USERS =================

    if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
      List<String> userIds = request.getUserIds();
      if (userIds == null || userIds.isEmpty()) return savedMaterial;

      // 🔥 fetch once
      List<TrainingAssignment> existing = assignmentRepo.findByTrainingId(savedMaterial.getId());

      Set<String> existingUserIds =
          existing.stream().map(TrainingAssignment::getUserId).collect(Collectors.toSet());

      // 🔥 bulk insert
      List<TrainingAssignment> batch = new ArrayList<>();

      for (String userId : userIds) {
        if (existingUserIds.contains(userId)) continue;

        batch.add(
            TrainingAssignment.builder()
                .userId(userId)
                .trainingId(savedMaterial.getId())
                .progress(0)
                .status("not-started")
                .dueDate(request.getDueDate())
                .assignedAt(Instant.now())
                .build());
      }

      if (!batch.isEmpty()) {
        assignmentRepo.saveAll(batch);
      }

      // 🔥 single count update
      savedMaterial.setAssignedTo((int) assignmentRepo.countByTrainingId(savedMaterial.getId()));
      materialRepo.save(savedMaterial);

      // 🔥 async notification
      fcmService.notifyTrainingAssigned(
          savedMaterial.getId(),
          savedMaterial.getTitle(),
          batch.stream().map(TrainingAssignment::getUserId).toList());
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
    List<TrainingAssignment> existing = assignmentRepo.findByTrainingId(trainingId);
    Set<String> existingUserIds =
        existing.stream().map(TrainingAssignment::getUserId).collect(Collectors.toSet());

    List<TrainingAssignment> batch = new ArrayList<>();

    for (String userId : userIds) {
      if (existingUserIds.contains(userId)) continue;

      batch.add(
          TrainingAssignment.builder()
              .userId(userId)
              .trainingId(trainingId)
              .progress(0)
              .status("not-started")
              .dueDate(dueDate)
              .assignedAt(Instant.now())
              .build());
    }

    if (!batch.isEmpty()) {
      assignmentRepo.saveAll(batch);
    }

    materialRepo
        .findById(trainingId)
        .ifPresent(
            material -> {
              material.setAssignedTo((int) assignmentRepo.countByTrainingId(trainingId));
              materialRepo.save(material);

              fcmService.notifyTrainingAssigned(
                  trainingId,
                  material.getTitle(),
                  batch.stream().map(TrainingAssignment::getUserId).toList());
            });
  }

  // ================= USER =================

  public List<TrainingAssignment> getUserTrainings(String userId) {
    return assignmentRepo.findByUserId(userId);
  }

  public List<UserTrainingDTO> getUserTrainingDetails(String userId) {

    return assignmentRepo.findByUserId(userId).stream()
        .map(
            assignment -> {
              TrainingMaterial material =
                  materialRepo.findByIdAndActiveTrue(assignment.getTrainingId()).orElse(null);

              if (material == null) return null;

              return new UserTrainingDTO(
                  assignment.getId(),
                  material.getId(),
                  material.getTitle(),
                  material.getType(),
                  material.getDuration(),
                  material.getDurationSeconds(),

                  // ✅ VIDEO (GENERIC)
                  material.getVideoProvider(),
                  material.getVideoPublicId(),
                  material.getVideoPlaybackUrl(),
                  material.getVideoFormat(),
                  material.getDocumentUrl(),
                  assignment.getProgress(),
                  assignment.getStatus(),
                  assignment.getDueDate());
            })
        .filter(Objects::nonNull)
        .toList();
  }

  public TrainingAssignment updateProgress(String userId, String trainingId, int progress) {

    TrainingAssignment assignment =
        assignmentRepo
            .findByUserIdAndTrainingId(userId, trainingId)
            .orElseThrow(() -> new RuntimeException("Training not assigned"));

    if (progress > assignment.getProgress()) {
      assignment.setProgress(progress);
    }

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
    return assignmentRepo.fetchEngagement(trainingId);
  }

  // ================= UPDATE =================

  public TrainingMaterial updateTraining(String trainingId, TrainingUploadAssignDTO request) {

    TrainingMaterial material =
        materialRepo
            .findById(trainingId)
            .orElseThrow(() -> new RuntimeException("Training not found"));

    /* ---------- BASIC FIELDS ---------- */
    if (request.getMaterial() != null) {
      if (request.getMaterial().getTitle() != null)
        material.setTitle(request.getMaterial().getTitle());

      if (request.getMaterial().getRegion() != null)
        material.setRegion(request.getMaterial().getRegion().toLowerCase());

      if (request.getMaterial().getType() != null)
        material.setType(request.getMaterial().getType());

      if (request.getMaterial().getDuration() != null)
        material.setDuration(request.getMaterial().getDuration());

      // ✅ ADD THIS
      if (request.getMaterial().getDurationSeconds() != null)
        material.setDurationSeconds(request.getMaterial().getDurationSeconds());
    }

    String type = material.getType();

    /* ---------- VIDEO ---------- */
    if ("video".equalsIgnoreCase(type)) {

      material.setVideoProvider("bunny");

      if (request.getVideoPublicId() != null) material.setVideoPublicId(request.getVideoPublicId());

      if (request.getVideoPlaybackUrl() != null)
        material.setVideoPlaybackUrl(request.getVideoPlaybackUrl());

      if (request.getVideoFormat() != null) material.setVideoFormat(request.getVideoFormat());

      material.setDocumentUrl(null);
    }

    /* ---------- DOCUMENT ---------- */
    else if ("document".equalsIgnoreCase(type)) {

      if (request.getDocumentUrl() != null) material.setDocumentUrl(request.getDocumentUrl());

      material.setVideoProvider(null);
      material.setVideoPublicId(null);
      material.setVideoPlaybackUrl(null);
      material.setVideoFormat(null);
      material.setDuration(null);
    }

    materialRepo.save(material);

    /* ---------- ASSIGNMENT UPDATE ---------- */

    List<String> newUserIds = request.getUserIds() != null ? request.getUserIds() : List.of();

    List<TrainingAssignment> existingAssignments = assignmentRepo.findByTrainingId(trainingId);

    Set<String> existingUserIds =
        existingAssignments.stream().map(TrainingAssignment::getUserId).collect(Collectors.toSet());

    /* ---------- ADD NEW ASSIGNMENTS (BULK) ---------- */
    List<TrainingAssignment> batch = new ArrayList<>();

    for (String userId : newUserIds) {
      if (existingUserIds.contains(userId)) continue;

      batch.add(
          TrainingAssignment.builder()
              .trainingId(trainingId)
              .userId(userId)
              .progress(0)
              .status("not-started")
              .dueDate(request.getDueDate())
              .assignedAt(Instant.now())
              .build());
    }

    if (!batch.isEmpty()) {
      assignmentRepo.saveAll(batch);
    }

    /* ---------- REMOVE UNASSIGNED USERS ---------- */
    for (TrainingAssignment assignment : existingAssignments) {
      if (!newUserIds.contains(assignment.getUserId())) {
        assignmentRepo.delete(assignment);
      } else {
        // update due date for still-assigned users
        assignment.setDueDate(request.getDueDate());
        assignmentRepo.save(assignment);
      }
    }

    /* ---------- UPDATE COUNT ---------- */
    material.setAssignedTo((int) assignmentRepo.countByTrainingId(trainingId));
    materialRepo.save(material);

    /* ---------- NOTIFY ONLY NEW USERS ---------- */
    if (!batch.isEmpty()) {
      fcmService.notifyTrainingAssigned(
          trainingId,
          material.getTitle(),
          batch.stream().map(TrainingAssignment::getUserId).toList());
    }

    return material;
  }

  public void deleteTraining(String trainingId) {

    TrainingMaterial material =
        materialRepo
            .findById(trainingId)
            .orElseThrow(() -> new RuntimeException("Training not found"));

    material.setActive(false);
    material.setDeletedAt(Instant.now());
    materialRepo.save(material);
  }

  public TrainingEditDTO getTrainingById(String trainingId) {

    TrainingMaterial material =
        materialRepo
            .findByIdAndActiveTrue(trainingId)
            .orElseThrow(() -> new RuntimeException("Training not found"));

    return TrainingMapper.toEditDTO(material, assignmentRepo.findByTrainingId(trainingId));
  }

  public List<TrainingEngagementDTO> getEngagementByTrainingId(String trainingId) {

    // optional: validate training exists
    materialRepo
        .findByIdAndActiveTrue(trainingId)
        .orElseThrow(() -> new RuntimeException("Training not found"));

    return assignmentRepo.fetchEngagement(trainingId);
  }
}
