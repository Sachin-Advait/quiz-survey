package com.gissoftware.quiz_survey.mapper;

import com.gissoftware.quiz_survey.dto.QuizzesSurveysDTO;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuizSurveyMapper {

  private final ResponseRepo responseRepo;

  public QuizzesSurveysDTO mapToDtoWithoutUser(QuizSurveyModel quiz) {
    return QuizzesSurveysDTO.builder()
        .id(quiz.getId())
        .type(quiz.getType())
        .title(quiz.getTitle())
        .totalQuestion(getQuestionCount(quiz))
        .status(quiz.getStatus())
        .quizTotalDuration(quiz.getQuizTotalDuration())
        .isAnnounced(quiz.getIsAnnounced())
        .createdAt(quiz.getCreatedAt())
        .maxRetake(quiz.getMaxRetake())
        .userDataDisplayFields(quiz.getUserDataDisplayFields())
        .visibilityType(quiz.getVisibilityType())
        .targetedUsers(quiz.getTargetedUsers()) // ADD THIS LINE
        .build();
  }

  public QuizzesSurveysDTO mapToDtoWithUser(QuizSurveyModel quiz, String userId) {

    boolean isParticipated =
        responseRepo.findByQuizSurveyIdAndUserId(quiz.getId(), userId).stream()
            .anyMatch(r -> r.getScore() != null || r.getAnswers() != null);
    boolean isMandatory = !isParticipated && Boolean.TRUE.equals(quiz.getIsMandatory());

    // Calculate remaining retakes for THIS user
    long userAttempts =
        responseRepo.findByQuizSurveyIdAndUserId(quiz.getId(), userId).stream()
            .filter(r -> r.getScore() != null)
            .count();

    int remainingRetakes = quiz.getMaxRetake() - (int) userAttempts;
    if (remainingRetakes < 0) remainingRetakes = 0;

    return QuizzesSurveysDTO.builder()
        .id(quiz.getId())
        .type(quiz.getType())
        .title(quiz.getTitle())
        .totalQuestion(getQuestionCount(quiz))
        .status(quiz.getStatus())
        .quizTotalDuration(quiz.getQuizTotalDuration())
        .quizDuration(quiz.getQuizDuration())
        .isAnnounced(quiz.getIsAnnounced())
        .createdAt(quiz.getCreatedAt())
        .isParticipated(isParticipated)
        .isMandatory(isMandatory)
        .maxRetake(remainingRetakes) // ✅ Now per-user retakes
        .userDataDisplayFields(quiz.getUserDataDisplayFields())
        .visibilityType(quiz.getVisibilityType())
        .targetedUsers(quiz.getTargetedUsers())
        .build();
  }

  private int getQuestionCount(QuizSurveyModel quiz) {
    if (quiz.getDefinitionJson() == null
        || quiz.getDefinitionJson().getPages() == null
        || quiz.getDefinitionJson().getPages().isEmpty()
        || quiz.getDefinitionJson().getPages().get(0).getElements() == null) {
      return 0;
    }
    return quiz.getDefinitionJson().getPages().get(0).getElements().size();
  }
}
