package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.Utils.ScoringUtil;
import com.gissoftware.quiz_survey.dto.AttemptResponseDTO;
import com.gissoftware.quiz_survey.dto.SaveAnswersRequest;
import com.gissoftware.quiz_survey.model.*;
import com.gissoftware.quiz_survey.repository.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AttemptService {

  private static final Logger log = LoggerFactory.getLogger(AttemptService.class);
  private final ResponseRepo responseRepo;
  private final QuizAttemptRepository attemptRepo;
  private final QuizSurveyRepository quizSurveyRepo;
  private final UserRepository userRepository;

  public AttemptResponseDTO startOrResumeAttempt(String quizSurveyId, String userId) {
    UserModel user =
        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Invalid userId"));

    QuizSurveyModel quiz =
        quizSurveyRepo
            .findById(quizSurveyId)
            .orElseThrow(() -> new IllegalArgumentException("Quiz/Survey not found"));

    if (!quiz.getStatus()) {
      throw new IllegalStateException("This quiz is no longer active");
    }

    if (quiz.getCreatedAt() != null && quiz.getQuizTotalDuration() != null) {
      long validityMinutes = Long.parseLong(quiz.getQuizTotalDuration());
      Instant expiryTime = quiz.getCreatedAt().plus(validityMinutes, ChronoUnit.MINUTES);
      if (Instant.now().isAfter(expiryTime)) {
        attemptRepo.findByQuizSurveyIdAndUserId(quizSurveyId, userId).stream()
            .filter(a -> a.isInProgress())
            .findFirst()
            .ifPresent(a -> submitExpiredAttempt(a, quiz));

        throw new IllegalStateException("This quiz has expired");
      }
    }

    Optional<QuizAttempt> existingInProgress =
        attemptRepo.findByQuizSurveyIdAndUserId(quizSurveyId, userId).stream()
            .filter(a -> a.isInProgress())
            .findFirst();

    if (existingInProgress.isPresent()) {
      QuizAttempt attempt = existingInProgress.get();

      int totalDurationMinutes = attempt.getDurationMinutes();
      int timeSpent = attempt.getTimeSpent() != null ? attempt.getTimeSpent() : 0;
      int remainingSeconds = Math.max(0, (totalDurationMinutes * 60) - timeSpent);

      if (remainingSeconds <= 0) {
        return AttemptResponseDTO.builder()
            .attemptId(attempt.getId())
            .status("EXPIRED")
            .message("Time has expired. Please submit your answers.")
            .answers(attempt.getAnswers())
            .build();
      }

      return AttemptResponseDTO.builder()
          .attemptId(attempt.getId())
          .status(attempt.getStatus().name())
          .message("Quiz attempt resumed")
          .startTime(attempt.getStartTime())
          .durationMinutes(attempt.getDurationMinutes())
          .remainingSeconds(remainingSeconds)
          .answers(attempt.getAnswers())
          .currentPageNo(attempt.getCurrentPageNo())
          .attemptNumber(attempt.getAttemptNumber())
          .timeSpent(timeSpent)
          .build();
    }

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

    boolean isRetake = completedAttempts > 0;

    int durationMinutes = parseIntSafe(quiz.getQuizDuration(), 30);
    int validityMinutes = parseIntSafe(quiz.getQuizTotalDuration(), 10080);

    Instant now = Instant.now();
    Instant endTime = now.plus(durationMinutes, ChronoUnit.MINUTES);
    Instant validityEndTime =
        quiz.getCreatedAt() != null
            ? quiz.getCreatedAt().plus(validityMinutes, ChronoUnit.MINUTES)
            : now.plus(validityMinutes, ChronoUnit.MINUTES);

    QuizAttempt newAttempt =
        QuizAttempt.builder()
            .quizSurveyId(quizSurveyId)
            .userId(userId)
            .username(user.getUsername())
            .startTime(now)
            .endTime(endTime)
            .validityEndTime(validityEndTime)
            .durationMinutes(durationMinutes)
            .totalValidityDays(validityMinutes / 1440)
            .answers(new HashMap<>())
            .currentPageNo(0)
            .status(AttemptStatus.IN_PROGRESS)
            .attemptNumber((int) (completedAttempts + 1))
            .timeSpent(0)
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
        .remainingSeconds((durationMinutes * 60))
        .answers(saved.getAnswers())
        .currentPageNo(0)
        .attemptNumber(saved.getAttemptNumber())
        .timeSpent(0)
        .build();
  }

  public void saveAnswers(String quizSurveyId, SaveAnswersRequest request) {
    QuizAttempt attempt =
        attemptRepo.findByQuizSurveyIdAndUserId(quizSurveyId, request.getUserId()).stream()
            .filter(a -> a.isInProgress())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No active attempt found"));

    if (!attempt.isInProgress()) {
      throw new IllegalStateException("Attempt is not in progress. Status: " + attempt.getStatus());
    }

    attempt.setAnswers(request.getAnswers());
    attempt.setCurrentPageNo(request.getCurrentPageNo());
    attempt.setPlatform(request.getPlatform());
    attempt.setClient(request.getClient());

    if (request.getTimeSpent() != null) {
      attempt.setTimeSpent(request.getTimeSpent());
    }

    attemptRepo.save(attempt);
  }

  private int parseIntSafe(String value, int defaultVal) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException | NullPointerException e) {
      return defaultVal;
    }
  }

  @Scheduled(fixedRate = 60000)
  @Scheduled(fixedRate = 60000)
  public void autoSubmitExpiredAttempts() {
    List<QuizAttempt> inProgressAttempts = attemptRepo.findByStatus(AttemptStatus.IN_PROGRESS);

    for (QuizAttempt attempt : inProgressAttempts) {
      try {
        QuizSurveyModel quiz = quizSurveyRepo.findById(attempt.getQuizSurveyId()).orElse(null);
        if (quiz == null) continue;

        int totalSeconds = attempt.getDurationMinutes() * 60;
        int timeSpent = attempt.getTimeSpent() != null ? attempt.getTimeSpent() : 0;

        if (timeSpent >= totalSeconds) {
          submitExpiredAttempt(attempt, quiz);
          continue;
        }

        if (quiz.getQuizTotalDuration() != null) {
          long validityMinutes = Long.parseLong(quiz.getQuizTotalDuration());
          Instant quizExpiry = quiz.getCreatedAt().plus(validityMinutes, ChronoUnit.MINUTES);
          if (Instant.now().isAfter(quizExpiry)) {
            submitExpiredAttempt(attempt, quiz);

            // ✅ Disable the quiz since validity has expired
            if (quiz.getStatus()) {
              quiz.setStatus(false);
              quizSurveyRepo.save(quiz);
              log.info("Quiz {} deactivated - validity expired", quiz.getId());
            }
          }
        }
      } catch (Exception e) {
        log.error("Failed to auto-submit attempt: {}", attempt.getId(), e);
      }
    }

    // ✅ Also deactivate quizzes with no attempts that have expired
    List<QuizSurveyModel> activeQuizzes = quizSurveyRepo.findByStatus(true);
    for (QuizSurveyModel quiz : activeQuizzes) {
      if (quiz.getQuizTotalDuration() != null && quiz.getCreatedAt() != null) {
        long validityMinutes = Long.parseLong(quiz.getQuizTotalDuration());
        Instant quizExpiry = quiz.getCreatedAt().plus(validityMinutes, ChronoUnit.MINUTES);
        if (Instant.now().isAfter(quizExpiry)) {
          quiz.setStatus(false);
          quizSurveyRepo.save(quiz);
          log.info("Quiz {} deactivated - validity expired (no attempts)", quiz.getId());
        }
      }
    }
  }

  private void submitExpiredAttempt(QuizAttempt attempt, QuizSurveyModel quiz) {
    Map<String, Object> given =
        attempt.getAnswers() != null ? attempt.getAnswers() : new HashMap<>();
    Map<String, Object> answerKey =
        quiz.getAnswerKey() != null ? quiz.getAnswerKey() : new HashMap<>();

    Integer score = null;
    Integer maxScore = null;

    if ("quiz".equalsIgnoreCase(quiz.getType())) {
      Map<String, String> questionTypes = new HashMap<>();
      Map<String, Integer> questionMarks = new HashMap<>();

      if (quiz.getDefinitionJson() != null && quiz.getDefinitionJson().getPages() != null) {
        quiz.getDefinitionJson()
            .getPages()
            .forEach(
                page ->
                    page.getElements()
                        .forEach(
                            el -> {
                              questionTypes.put(el.getName(), el.getType());
                              questionMarks.put(
                                  el.getName(), el.getMarks() != null ? el.getMarks() : 1);
                            }));
      }

      ScoringUtil.ScoringResult result =
          ScoringUtil.score(given, answerKey, questionTypes, questionMarks);
      score = result.score();
      maxScore = quiz.getMaxScore();
    }

    ResponseModel response =
        ResponseModel.builder()
            .quizSurveyId(quiz.getId())
            .userId(attempt.getUserId())
            .username(attempt.getUsername())
            .answers(attempt.getAnswers())
            .score(score)
            .maxScore(maxScore)
            .finishTime(
                java.time.Duration.ofSeconds(
                    attempt.getTimeSpent() != null ? attempt.getTimeSpent() : 0))
            .submittedAt(Instant.now())
            .platform(attempt.getPlatform())
            .client(attempt.getClient())
            .build();

    responseRepo.save(response);
    attempt.setStatus(AttemptStatus.AUTO_SUBMITTED);
    attemptRepo.save(attempt);

    log.info("Auto-submitted expired attempt: {} for quiz: {}", attempt.getId(), quiz.getId());
  }
}
