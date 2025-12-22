package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.TrainingEngagementDTO;
import com.gissoftware.quiz_survey.dto.UserTrainingDTO;
import com.gissoftware.quiz_survey.model.TrainingAssignment;
import com.gissoftware.quiz_survey.model.TrainingMaterial;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.TrainingAssignmentRepository;
import com.gissoftware.quiz_survey.repository.TrainingMaterialRepository;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingMaterialRepository materialRepo;
    private final TrainingAssignmentRepository assignmentRepo;
    private final UserRepository userRepo;
    private final FCMService fcmService;

    // ================= ADMIN =================
    public TrainingMaterial uploadTraining(TrainingMaterial material) {
        material.setAssignedTo(0);
        material.setCompletionRate(0);
        material.setViews(0);
        material.setUploadDate(Instant.now());
        return materialRepo.save(material);
    }

    public List<TrainingMaterial> getAllMaterials() {
        return materialRepo.findAll();
    }

    public List<TrainingMaterial> getMaterialsByRegion(String region) {
        if ("all".equalsIgnoreCase(region)) {
            return materialRepo.findAll();
        }
        return materialRepo.findByRegion(region);
    }

    public void assignTraining(String trainingId, List<String> userIds, Instant dueDate) {
        for (String userId : userIds) {
            boolean alreadyAssigned = assignmentRepo.findByUserIdAndTrainingId(userId, trainingId).isPresent();
            if (alreadyAssigned) continue;

            TrainingAssignment assignment = TrainingAssignment.builder()
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
        materialRepo.findById(trainingId).ifPresent(material -> {
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
                            TrainingMaterial material = materialRepo
                                    .findById(assignment.getTrainingId())
                                    .orElseThrow(() -> new RuntimeException("Training not found"));

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
                .toList();
    }

    public TrainingAssignment updateProgress(String userId, String trainingId, int progress) {
        TrainingAssignment assignment =
                assignmentRepo
                        .findByUserIdAndTrainingId(userId, trainingId)
                        .orElseThrow(() -> new RuntimeException("Training not assigned"));

        int current = assignment.getProgress();

        if (progress > current) {
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

        List<TrainingAssignment> assignments;
        if (trainingId != null) assignments = assignmentRepo.findByTrainingId(trainingId);
        else assignments = assignmentRepo.findAll();

        return assignments.stream()
                .map(a -> {
                    UserModel user = userRepo.findById(a.getUserId()).orElse(null);
                    TrainingMaterial material = materialRepo.findById(a.getTrainingId()).orElse(null);

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
}
