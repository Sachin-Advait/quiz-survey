package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.ParticipationStatusDTO;
import com.gissoftware.quiz_survey.model.*;
import com.gissoftware.quiz_survey.repository.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ParticipationService {

  private final QuizSurveyRepository quizSurveyRepository;
  private final ResponseRepo responseRepository;
  private final UserRepository userRepository;

  public List<ParticipationStatusDTO> getParticipationStatus(String quizSurveyId) {
    QuizSurveyModel qs =
        quizSurveyRepository
            .findById(quizSurveyId)
            .orElseThrow(() -> new RuntimeException("QuizSurvey not found: " + quizSurveyId));

    String type = qs.getType();
    boolean isQuiz = "quiz".equalsIgnoreCase(type);

    List<ResponseModel> responses = responseRepository.findByQuizSurveyId(quizSurveyId);

    Map<String, ResponseModel> responseByUserId =
        responses.stream()
            .filter(
                r ->
                    "survey".equalsIgnoreCase(type)
                        ? r.getSubmittedAt() != null
                        : r.getScore() != null)
            .collect(
                Collectors.toMap(
                    ResponseModel::getUserId,
                    r -> r,
                    (r1, r2) -> r1.getSubmittedAt().isAfter(r2.getSubmittedAt()) ? r1 : r2));
    List<String> targetedUserIds = qs.getTargetedUsers();
    if (targetedUserIds == null || targetedUserIds.isEmpty()) return List.of();

    List<UserModel> users = userRepository.findAllById(targetedUserIds);
    Map<String, UserModel> userById =
        users.stream().collect(Collectors.toMap(UserModel::getId, u -> u, (a, b) -> a));

    // Extract questions from definition
    List<SurveyDefinition.Element> elements = new ArrayList<>();
    if (qs.getDefinitionJson() != null && qs.getDefinitionJson().getPages() != null) {
      for (SurveyDefinition.Page page : qs.getDefinitionJson().getPages()) {
        if (page.getElements() != null) {
          elements.addAll(page.getElements());
        }
      }
    }

    Map<String, Object> answerKey = qs.getAnswerKey() != null ? qs.getAnswerKey() : Map.of();

    List<ParticipationStatusDTO> result = new ArrayList<>();

    for (String userId : targetedUserIds) {
      UserModel user = userById.get(userId);
      String staffId = user != null ? user.getStaffId() : userId;
      String username = user != null ? user.getUsername() : "";
      String region = user != null ? user.getRegion() : "";
      String outlet = user != null ? user.getOutlet() : "";
      String position = user != null ? user.getPosition() : "";

      boolean participated = responseByUserId.containsKey(userId);
      ResponseModel response = responseByUserId.get(userId);

      Integer score = response != null ? response.getScore() : null;
      Integer maxScore = response != null ? response.getMaxScore() : null;
      Double pct =
          (score != null && maxScore != null && maxScore > 0) ? (score * 100.0 / maxScore) : null;
      String res = computeResult(type, participated, score, maxScore);

      if (!participated || elements.isEmpty()) {
        // Single row — no question detail
        result.add(
            ParticipationStatusDTO.builder()
                .userId(userId)
                .staffId(staffId)
                .username(username)
                .region(region)
                .outlet(outlet)
                .position(position)
                .title(qs.getTitle())
                .participated(participated)
                .score(score)
                .maxScore(maxScore)
                .percentage(pct)
                .result(res)
                .build());
      } else {
        // One row per question
        Map<String, Object> answers =
            response.getAnswers() != null ? response.getAnswers() : Map.of();

        for (SurveyDefinition.Element el : elements) {
          String qName = el.getName();
          String qTitle = el.getTitle() != null ? el.getTitle() : qName;
          String arabicTitle = el.getArabicTitle();

          Object agentAnsObj = answers.get(qName);
          String agentAns = agentAnsObj != null ? agentAnsObj.toString() : "";

          Object correctAnsObj = answerKey.get(qName);
          String correctAns = correctAnsObj != null ? correctAnsObj.toString() : "";

          boolean questionCompletion = agentAnsObj != null;

          result.add(
              ParticipationStatusDTO.builder()
                  .userId(userId)
                  .staffId(staffId)
                  .username(username)
                  .region(region)
                  .outlet(outlet)
                  .position(position)
                  .title(qs.getTitle())
                  .participated(true)
                  .score(score)
                  .maxScore(maxScore)
                  .percentage(pct)
                  .marks(el.getMarks())
                  .result(res)
                  .question(qTitle)
                  .agentAnswer(agentAns)
                  .correctAnswer(correctAns)
                  .arabicTitle(arabicTitle)
                  .completion(questionCompletion)
                  .quizOpenTime(qs.getCreatedAt())
                  .agentOpenTime(response.getOpenedAt())
                  .agentSubmissionTime(response.getSubmittedAt())
                  .build());
        }
      }
    }

    return result;
  }

  private String computeResult(String type, boolean participated, Integer score, Integer maxScore) {
    if (!participated) return "NOT_SUBMITTED";
    if ("survey".equalsIgnoreCase(type)) return "SUBMITTED";
    if (score != null && maxScore != null && maxScore > 0) {
      return (score * 100.0 / maxScore) >= 60 ? "PASS" : "FAIL";
    }
    return "SUBMITTED";
  }
}
