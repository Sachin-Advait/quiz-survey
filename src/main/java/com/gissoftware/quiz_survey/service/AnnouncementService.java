package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.AnnouncementWithReadStatus;
import com.gissoftware.quiz_survey.model.AnnouncementMode;
import com.gissoftware.quiz_survey.model.AnnouncementModel;
import com.gissoftware.quiz_survey.model.AnnouncementRead;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.repository.AnnouncementReadRepository;
import com.gissoftware.quiz_survey.repository.AnnouncementRepository;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

  private final AnnouncementRepository announcementRepo;
  private final AnnouncementReadRepository readRepo;
  private final QuizSurveyRepository quizSurveyRepository;
  private final UserRepository userRepository;
  private final FCMService fcmService;

  public AnnouncementModel create(String quizSurveyId, String message) {

    QuizSurveyModel quizSurveyModel =
        quizSurveyRepository
            .findById(quizSurveyId)
            .orElseThrow(() -> new RuntimeException("Invalid quiz survey Id"));

    if (quizSurveyModel.getAnnouncementMode() == AnnouncementMode.SCHEDULED) {
      quizSurveyModel.setIsAnnounced(true);
      quizSurveyRepository.save(quizSurveyModel);
    } else if (quizSurveyModel.getAnnouncementMode() == AnnouncementMode.IMMEDIATE) {
      throw new IllegalArgumentException("Quiz Survey already announced");
    }

    AnnouncementModel saved =
        announcementRepo.save(
            AnnouncementModel.builder().title(quizSurveyModel.getTitle()).message(message).build());

    // 🔥 Send push notification to ALL users
    userRepository
        .findAll()
        .forEach(
            user -> {
              if (user.getFcmToken() != null) {
                fcmService.sendNotification(
                    user.getFcmToken(),
                    saved.getTitle(),
                    saved.getMessage(),
                    "NOTIFICATION", // Category used by your service worker
                    saved.getId() // Will be "?id=savedId"
                    );
              }
            });

    return saved;
  }

  public List<AnnouncementWithReadStatus> getAllWithReadStatus(String userId) {
    Set<String> readIds =
        readRepo.findById(userId).map(AnnouncementRead::getAnnouncementIds).orElse(Set.of());

    return announcementRepo.findAll().stream()
        // Filter: show if targetUser is null/empty (general) OR includes this user
        .filter(
            a ->
                a.getTargetUser() == null
                    || a.getTargetUser().isEmpty()
                    || a.getTargetUser().contains(userId))
        .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
        .map(
            a ->
                new AnnouncementWithReadStatus(
                    a.getId(),
                    a.getTitle(),
                    a.getMessage(),
                    a.getCreatedAt(),
                    readIds.contains(a.getId())))
        .toList();
  }

  public void markAsRead(String userId, String announcementId) {
    AnnouncementRead read =
        readRepo.findById(userId).orElse(AnnouncementRead.builder().userId(userId).build());

    read.getAnnouncementIds().add(announcementId);
    readRepo.save(read);
  }

  public void markAllAsRead(String userId) {
    userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Invalid username"));

    List<AnnouncementModel> announcements = announcementRepo.findAll();
    Set<String> allIds =
        announcements.stream().map(AnnouncementModel::getId).collect(Collectors.toSet());

    AnnouncementRead read =
        readRepo
            .findById(userId)
            .orElse(
                AnnouncementRead.builder()
                    .userId(userId)
                    .announcementIds(new HashSet<>()) // 👈 Initialize here
                    .build());

    // Ensure it's initialized even if found from DB but null inside
    if (read.getAnnouncementIds() == null) {
      read.setAnnouncementIds(new HashSet<>());
    }

    read.getAnnouncementIds().addAll(allIds);
    readRepo.save(read);
  }

  public AnnouncementModel createWithTargets(
      String quizSurveyId, String message, List<String> targetUser) {

    String title = "System Notification"; // Default title

    if (quizSurveyId != null && !quizSurveyId.isEmpty()) {
      QuizSurveyModel quizSurveyModel =
          quizSurveyRepository
              .findById(quizSurveyId)
              .orElseThrow(() -> new RuntimeException("Invalid quiz survey Id"));

      quizSurveyModel.setIsAnnounced(true);
      quizSurveyRepository.save(quizSurveyModel);

      title = quizSurveyModel.getTitle();
    }

    AnnouncementModel saved =
        announcementRepo.save(
            AnnouncementModel.builder()
                .title(title)
                .message(message)
                .targetUser(targetUser)
                .build());

    // 🔥 Send notification ONLY to selected users
    targetUser.forEach(
        userId -> {
          userRepository
              .findById(userId)
              .ifPresent(
                  user -> {
                    if (user.getFcmToken() != null) {
                      fcmService.sendNotification(
                          user.getFcmToken(),
                          saved.getTitle(),
                          saved.getMessage(),
                          "NOTIFICATION",
                          saved.getId());
                    }
                  });
        });

    return saved;
  }
}
