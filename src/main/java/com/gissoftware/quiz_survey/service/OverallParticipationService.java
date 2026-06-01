package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.OverallParticipationDTO;
import com.gissoftware.quiz_survey.model.*;
import com.gissoftware.quiz_survey.repository.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OverallParticipationService {

  private final QuizSurveyRepository quizSurveyRepository;
  private final ResponseRepo responseRepository;
  private final UserRepository userRepository;

  public List<OverallParticipationDTO> getOverallParticipation() {

    List<QuizSurveyModel> allQuizSurveys = quizSurveyRepository.findAll();
    List<ResponseModel> allResponses = responseRepository.findAll();
    List<UserModel> allUsers = userRepository.findAll();

    Map<String, UserModel> userById =
        allUsers.stream().collect(Collectors.toMap(UserModel::getId, u -> u, (a, b) -> a));

    Map<String, List<ResponseModel>> responsesByQuiz =
        allResponses.stream().collect(Collectors.groupingBy(ResponseModel::getQuizSurveyId));

    List<OverallParticipationDTO> result = new ArrayList<>();

    for (QuizSurveyModel qs : allQuizSurveys) {

      String qsId = qs.getId();
      String type = qs.getType();
      List<ResponseModel> responses = responsesByQuiz.getOrDefault(qsId, List.of());

      Set<String> respondedUserIds =
          responses.stream().map(ResponseModel::getUserId).collect(Collectors.toSet());

      List<String> targetedUserIds = qs.getTargetedUsers();
      if (targetedUserIds == null || targetedUserIds.isEmpty()) continue;

      // Extract all questions from definition once per quiz/survey
      List<SurveyDefinition.Element> elements = new ArrayList<>();
      if (qs.getDefinitionJson() != null && qs.getDefinitionJson().getPages() != null) {
        for (SurveyDefinition.Page page : qs.getDefinitionJson().getPages()) {
          if (page.getElements() != null) {
            elements.addAll(page.getElements());
          }
        }
      }

      for (String userId : targetedUserIds) {

        UserModel user = userById.get(userId);
        String staffId = user != null ? user.getStaffId() : userId;
        String username = user != null ? user.getUsername() : "";
        String region = user != null ? user.getRegion() : "";
        String outlet = user != null ? user.getOutlet() : "";

        boolean participated = respondedUserIds.contains(userId);

        Optional<ResponseModel> responseOpt =
            responses.stream()
                .filter(r -> r.getUserId().equals(userId))
                .max(Comparator.comparing(ResponseModel::getSubmittedAt));

        Integer score = responseOpt.map(ResponseModel::getScore).orElse(null);
        Integer maxScore = responseOpt.map(ResponseModel::getMaxScore).orElse(null);

        Double pct =
            (score != null && maxScore != null && maxScore > 0) ? (score * 100.0 / maxScore) : null;

        String res = computeResult(type, participated, score, maxScore);

        if (!participated || elements.isEmpty()) {

          result.add(
              OverallParticipationDTO.builder()
                  .quizSurveyId(qsId)
                  .title(qs.getTitle())
                  .type(type)
                  .userId(userId)
                  .staffId(staffId)
                  .username(username)
                  .region(region)
                  .outlet(outlet)
                  .participated(participated)
                  .score(score)
                  .maxScore(maxScore)
                  .percentage(pct)
                  .result(res)
                  .quizOpenTime(qs.getCreatedAt())
                  .agentOpenTime(responseOpt.map(ResponseModel::getOpenedAt).orElse(null))
                  .agentSubmissionTime(responseOpt.map(ResponseModel::getSubmittedAt).orElse(null))
                  .build());

        } else {

          ResponseModel response = responseOpt.get();
          Map<String, Object> answers =
              response.getAnswers() != null ? response.getAnswers() : Map.of();

          Map<String, String> questionAnswers = new LinkedHashMap<>();
          Map<String, String> correctAnswers = new LinkedHashMap<>();
          boolean completion = true;

          for (SurveyDefinition.Element el : elements) {

            String qName = el.getName();
            String qTitle = el.getTitle() != null ? el.getTitle() : qName;

            String header = qTitle;

            Object agentAnsObj = answers.get(qName);
            String agentAns = agentAnsObj != null ? agentAnsObj.toString() : "";

            if (agentAnsObj == null) {
              completion = false;
            }

            questionAnswers.put(header, agentAns);

            // Correct answer — only populated for quiz elements that have one
            String correctAns =
                el.getCorrectAnswer() != null ? el.getCorrectAnswer().toString() : "";
            correctAnswers.put(header, correctAns);
          }

          result.add(
              OverallParticipationDTO.builder()
                  .quizSurveyId(qsId)
                  .title(qs.getTitle())
                  .type(type)
                  .userId(userId)
                  .staffId(staffId)
                  .username(username)
                  .region(region)
                  .outlet(outlet)
                  .participated(true)
                  .score(score)
                  .maxScore(maxScore)
                  .percentage(pct)
                  .result(res)
                  .completion(completion)
                  .quizOpenTime(qs.getCreatedAt())
                  .agentOpenTime(response.getOpenedAt())
                  .agentSubmissionTime(response.getSubmittedAt())
                  .questionAnswers(questionAnswers)
                  .correctAnswers(correctAnswers)
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
