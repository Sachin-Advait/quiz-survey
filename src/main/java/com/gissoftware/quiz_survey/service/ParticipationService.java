package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.ParticipationStatusDTO;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipationService {

  private final QuizSurveyRepository quizSurveyRepo;
  private final ResponseRepo responseRepo;
  private final UserRepository userRepository;

  public List<ParticipationStatusDTO> getParticipationStatus(String quizSurveyId) {

    QuizSurveyModel qs =
        quizSurveyRepo
            .findById(quizSurveyId)
            .orElseThrow(() -> new RuntimeException("Quiz/Survey not found"));

    List<UserModel> users = userRepository.findAllById(qs.getTargetedUsers());
    List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);

    // Best attempt per user (highest score)
    Map<String, ResponseModel> bestResponse =
        responses.stream()
            .collect(
                Collectors.toMap(
                    ResponseModel::getUserId,
                    r -> r,
                    (r1, r2) -> {
                      if (r1.getScore() == null) return r1;
                      if (r2.getScore() == null) return r2;
                      return r1.getScore() >= r2.getScore() ? r1 : r2;
                    }));

    boolean isQuiz = qs.getType().equalsIgnoreCase("quiz");

    return users.stream()
        .map(
            user -> {
              ResponseModel resp = bestResponse.get(user.getId());
              boolean participated = resp != null;

              Integer score = null;
              Integer maxScore = null;
              String result;
              Double percentage = null;

              if (!participated) {
                result = isQuiz ? "NOT_ATTEMPTED" : "NOT_SUBMITTED";
              } else if (isQuiz) {
                score = resp.getScore();
                maxScore = resp.getMaxScore();

                if (score != null && maxScore != null) {
                  result = score >= 0.5 * maxScore ? "PASS" : "FAIL";
                } else {
                  result = "FAIL";
                }
              } else {
                result = "SUBMITTED";
              }
              if (isQuiz && score != null && maxScore != null && maxScore > 0) {
                percentage = (score * 100.0) / maxScore;
              }

              return ParticipationStatusDTO.builder()
                  .userId(user.getId())
                  .staffId(user.getStaffId())
                  .username(user.getUsername())
                  .region(user.getRegion())
                  .outlet(user.getOutlet())
                  .position(user.getPosition())
                  .participated(participated)
                  .score(score)
                  .maxScore(maxScore)
                  .percentage(percentage)
                  .result(result)
                  .build();
            })
        .toList();
  }
}
