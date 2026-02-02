package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.OverallParticipationDTO;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OverallParticipationService {

  private final QuizSurveyRepository quizSurveyRepo;
  private final ResponseRepo responseRepo;
  private final UserRepository userRepository;

  public List<OverallParticipationDTO> getOverallParticipation() {

    List<QuizSurveyModel> allQuizSurveys = quizSurveyRepo.findAll();
    List<ResponseModel> allResponses = responseRepo.findAll();

    // best attempt per user per quiz
    Map<String, ResponseModel> bestResponses =
        allResponses.stream()
            .collect(
                Collectors.toMap(
                    r -> r.getQuizSurveyId() + "_" + r.getUserId(),
                    r -> r,
                    (r1, r2) -> {
                      if (r1.getScore() == null) return r1;
                      if (r2.getScore() == null) return r2;
                      return r1.getScore() >= r2.getScore() ? r1 : r2;
                    }));

    List<OverallParticipationDTO> result = new ArrayList<>();

    for (QuizSurveyModel qs : allQuizSurveys) {
      boolean isQuiz = qs.getType().equalsIgnoreCase("quiz");

      List<UserModel> users = userRepository.findAllById(qs.getTargetedUsers());

      for (UserModel user : users) {
        String key = qs.getId() + "_" + user.getId();
        ResponseModel resp = bestResponses.get(key);

        boolean participated = resp != null;
        Integer score = null;
        Integer maxScore = null;
        String status;

        if (!participated) {
          status = isQuiz ? "NOT_ATTEMPTED" : "NOT_SUBMITTED";
        } else if (isQuiz) {
          score = resp.getScore();
          maxScore = resp.getMaxScore();
          status = score >= 0.5 * maxScore ? "PASS" : "FAIL";
        } else {
          status = "SUBMITTED";
        }

        result.add(
            OverallParticipationDTO.builder()
                .quizSurveyId(qs.getId())
                .title(qs.getTitle())
                .type(qs.getType())
                .userId(user.getId())
                .staffId(user.getStaffId())
                .username(user.getUsername())
                .participated(participated)
                .score(score)
                .maxScore(maxScore)
                .result(status)
                .build());
      }
    }
    return result;
  }
}
