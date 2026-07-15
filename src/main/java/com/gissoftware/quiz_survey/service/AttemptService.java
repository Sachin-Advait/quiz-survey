package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.AttemptResponseDTO;
import com.gissoftware.quiz_survey.dto.SaveAnswersRequest;
import com.gissoftware.quiz_survey.model.*;
import com.gissoftware.quiz_survey.repository.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttemptService {

  private final ResponseRepo responseRepo;
  private final QuizAttemptRepository attemptRepo;
  private final QuizSurveyRepository quizSurveyRepo;
  private final UserRepository userRepository;

  /** Start or resume quiz attempt Called when user opens TakeSurveyPage */
  public AttemptResponseDTO startOrResumeAttempt(String quizSurveyId, String userId) {
    // Validate user
    UserModel user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Invalid userId"));

    // Validate quiz exists and is active
    QuizSurveyModel quiz =
        quizSurveyRepo
            .findById(quizSurveyId)
            .orElseThrow(() -> new IllegalArgumentException("Quiz/Survey not found"));

    if (!quiz.getStatus()) {
      throw new IllegalStateException("This quiz is no longer active");
    }

    // Check if quiz validity has expired
    if (quiz.getCreatedAt() != null && quiz.getQuizTotalDuration() != null) {
      long validityDays = Long.parseLong(quiz.getQuizTotalDuration());
      Instant expiryTime = quiz.getCreatedAt().plus(validityDays, ChronoUnit.DAYS);
      if (Instant.now().isAfter(expiryTime)) {
        throw new IllegalStateException("This quiz has expired");
      }
    }

    // Check for existing IN_PROGRESS attempt (not completed)
    Optional<QuizAttempt> existingInProgress =
        attemptRepo.findByQuizSurveyIdAndUserId(quizSurveyId, userId).stream()
            .filter(a -> a.isInProgress())
            .findFirst();
    if (existingInProgress.isPresent()) {
      QuizAttempt attempt = existingInProgress.get();

      // If expired, tell frontend time is up
      if (attempt.isExpired()) {
        return AttemptResponseDTO.builder()
            .attemptId(attempt.getId())
            .status("EXPIRED")
            .message("Time has expired. Please submit your answers.")
            .answers(attempt.getAnswers())
            .endTime(attempt.getEndTime())
            .build();
      }

      // Return existing IN_PROGRESS attempt (resume)
      return AttemptResponseDTO.builder()
          .attemptId(attempt.getId())
          .status(attempt.getStatus().name())
          .message("Quiz attempt resumed")
          .startTime(attempt.getStartTime())
          .endTime(attempt.getEndTime())
          .durationMinutes(attempt.getDurationMinutes())
          .remainingSeconds(attempt.getRemainingSeconds())
          .answers(attempt.getAnswers())
          .currentPageNo(attempt.getCurrentPageNo())
          .attemptNumber(attempt.getAttemptNumber())
          .build();
    }

    // Check retake limits by counting completed ResponseModels
    long completedAttempts =
        responseRepo.findByQuizSurveyIdAndUserId(quizSurveyId, userId).stream()
            .filter(r -> r.getScore() != null)
            .count();

    if (quiz.getMaxRetake() != null && completedAttempts >= quiz.getMaxRetake()) {
      throw new IllegalStateException(
          "Maximum attempts ("
              + quiz.getMaxRetake()
              + ") reached. "
              + "You have completed "
              + completedAttempts
              + " attempt(s).");
    }

    // Check if user has previous attempts (for retake messaging)
    boolean isRetake = completedAttempts > 0;

    // Create new attempt
    int durationMinutes = parseIntSafe(quiz.getQuizDuration(), 30);
    int validityDays = parseIntSafe(quiz.getQuizTotalDuration(), 7);

    Instant now = Instant.now();
    Instant endTime = now.plus(durationMinutes, ChronoUnit.MINUTES);
    Instant validityEndTime =
        quiz.getCreatedAt() != null
            ? quiz.getCreatedAt().plus(validityDays, ChronoUnit.DAYS)
            : now.plus(validityDays, ChronoUnit.DAYS);

    QuizAttempt newAttempt =
        QuizAttempt.builder()
            .quizSurveyId(quizSurveyId)
            .userId(userId)
            .username(user.getUsername())
            .startTime(now)
            .endTime(endTime)
            .validityEndTime(validityEndTime)
            .durationMinutes(durationMinutes)
            .totalValidityDays(validityDays)
            .answers(new HashMap<>())
            .currentPageNo(0)
            .status(AttemptStatus.IN_PROGRESS)
            .attemptNumber((int) (completedAttempts + 1))
            .build();

    QuizAttempt saved = attemptRepo.save(newAttempt);

    String message =
        isRetake
            ? "Quiz retake started (Attempt "
                + saved.getAttemptNumber()
                + " of "
                + quiz.getMaxRetake()
                + ")"
            : "Quiz attempt started";

    return AttemptResponseDTO.builder()
        .attemptId(saved.getId())
        .status(saved.getStatus().name())
        .message(message)
        .startTime(saved.getStartTime())
        .endTime(saved.getEndTime())
        .durationMinutes(durationMinutes)
        .remainingSeconds(saved.getRemainingSeconds())
        .answers(saved.getAnswers())
        .currentPageNo(0)
        .attemptNumber(saved.getAttemptNumber())
        .build();
  }

  /** Save answers periodically during the attempt */
  public void saveAnswers(String quizSurveyId, SaveAnswersRequest request) {
    QuizAttempt attempt =
        attemptRepo.findByQuizSurveyIdAndUserId(quizSurveyId, request.getUserId()).stream()
            .filter(a -> a.isInProgress())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No active attempt found"));

    if (!attempt.isInProgress()) {
      throw new IllegalStateException("Attempt is not in progress. Status: " + attempt.getStatus());
    }

    if (attempt.isExpired()) {
      throw new IllegalStateException(
          "Time has expired for this attempt. Please submit your answers.");
    }

    attempt.setAnswers(request.getAnswers());
    attempt.setCurrentPageNo(request.getCurrentPageNo());
    attempt.setPlatform(request.getPlatform());
    attempt.setClient(request.getClient());

    attemptRepo.save(attempt);
  }

  private int parseIntSafe(String value, int defaultVal) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException | NullPointerException e) {
      return defaultVal;
    }
  }
}
