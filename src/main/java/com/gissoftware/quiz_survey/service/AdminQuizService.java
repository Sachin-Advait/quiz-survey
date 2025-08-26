package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.controller.QuizSurveySocketController;
import com.gissoftware.quiz_survey.dto.QuizCompletionStatsDTO;
import com.gissoftware.quiz_survey.dto.QuizInsightsDTO;
import com.gissoftware.quiz_survey.dto.QuizResponseByRegionDTO;
import com.gissoftware.quiz_survey.mapper.QuizSurveyMapper;
import com.gissoftware.quiz_survey.mapper.SurveyResponseStatsMapper;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.SurveyDefinition;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@AllArgsConstructor
@Service
public class AdminQuizService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final ResponseRepo responseRepo;
    private final UserRepository userRepository;
    private final QuizSurveySocketController quizSurveySocketController;
    private final QuizSurveyMapper quizSurveyMapper;
    private final SurveyResponseStatsMapper surveyResponseStatsMapper;


    private static boolean isCorrect(Object correctAnswer, Object userAnswer) {
        boolean correct = false;
        if (correctAnswer instanceof String str) {
            correct = str.equals(userAnswer);
        } else if (correctAnswer instanceof List<?> correctList) {
            if (userAnswer instanceof List<?> userList) {
                correct = correctList.containsAll(userList) && userList.containsAll(correctList);
            } else {
                correct = correctList.contains(userAnswer);
            }
        }
        return correct;
    }

    // Get Quiz Insights(Like Lowest and Highest Score)
    public QuizInsightsDTO getQuizInsights(String quizSurveyId) {
        QuizSurveyModel quiz = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz not found"));

        if (!quiz.getType().equalsIgnoreCase("quiz")) {
            throw new IllegalArgumentException("Insights are only available for quizzes");
        }

        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);
        if (responses.isEmpty()) {
            throw new IllegalArgumentException("No responses found for this quiz");
        }

        int totalScore = 0;
        int totalUsers = responses.size();
        int passCount = 0;
        int maxScore = quiz.getMaxScore() != null ? quiz.getMaxScore() : 100;

        ResponseModel top = null, low = null;
        Map<String, Integer> incorrectCountPerQuestion = new HashMap<>();
        Map<String, Object> answerKey = quiz.getAnswerKey();

        for (ResponseModel r : responses) {
            int score = r.getScore() != null ? r.getScore() : 0;
            totalScore += score;

            if (score >= 0.5 * maxScore) passCount++;

            if (top == null || (r.getScore() != null && r.getScore() > top.getScore())) {
                top = r;
            }
            if (low == null || (r.getScore() != null && r.getScore() < low.getScore())) {
                low = r;
            }

            Map<String, Object> userAnswers = r.getAnswers();

            for (Map.Entry<String, Object> entry : answerKey.entrySet()) {
                String qId = entry.getKey();
                Object correctAnswer = entry.getValue();
                Object userAnswer = userAnswers.get(qId);
                boolean correct = isCorrect(correctAnswer, userAnswer);

                if (!correct) {
                    incorrectCountPerQuestion.merge(qId, 1, Integer::sum);
                }
            }
        }

        // Get the most incorrectly answered questions (sorted by count desc)
        List<Map.Entry<String, Integer>> sortedIncorrect = incorrectCountPerQuestion.entrySet()
                .stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .toList();

        List<QuizInsightsDTO.MostIncorrectQuestionDTO> mostIncorrectQuestions = sortedIncorrect.stream()
                .limit(5)
                .map(entry -> {
                    String questionId = entry.getKey();

                    // Search across all pages and elements to find the matching question ID
                    String questionText = quiz.getDefinitionJson().getPages().stream()
                            .flatMap(page -> page.getElements().stream())
                            .filter(element -> questionId.equals(element.getName())) // Match ID
                            .map(SurveyDefinition.Element::getTitle)
                            .findFirst()
                            .orElse("Unknown Question"); // Fallback if not found

                    return new QuizInsightsDTO.MostIncorrectQuestionDTO(questionText, entry.getValue());
                })
                .collect(Collectors.toList());


        double average = (double) totalScore / totalUsers;
        double formattedAverage = Math.round(average * 100.0) / 100.0;

        return QuizInsightsDTO.builder()
                .title(quiz.getTitle())
                .averageScore(formattedAverage)
                .passRate(Double.parseDouble(String.format("%.2f", 100.0 * passCount / totalUsers)))
                .failRate(Double.parseDouble(String.format("%.2f", 100.0 * (totalUsers - passCount) / totalUsers)))
                .topScorer(new QuizInsightsDTO.ScorerDTO(top.getUsername(), top.getScore()))
                .lowestScorer(new QuizInsightsDTO.ScorerDTO(low.getUsername(), low.getScore()))
                .mostIncorrectQuestions(mostIncorrectQuestions)
                .build();
    }

    // Get Quiz Overall Completion Status
    public QuizCompletionStatsDTO getQuizCompletionStats(String quizId) {
        var quiz = quizSurveyRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        List<String> targetedUsers = quiz.getTargetedUsers();
        int totalAssigned = targetedUsers.size();

        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizId);
        List<String> completedUserIds = responses.stream()
                .map(ResponseModel::getUserId)
                .distinct()
                .toList();

        int totalCompleted = (int) targetedUsers.stream()
                .filter(completedUserIds::contains)
                .count();

        int totalNotCompleted = totalAssigned - totalCompleted;
        double completionRate = totalAssigned == 0 ? 0 :
                Math.round(((double) totalCompleted / totalAssigned) * 10000.0) / 100.0;

        return new QuizCompletionStatsDTO(totalAssigned, totalCompleted, totalNotCompleted, completionRate);
    }

    // Get Quiz Responses by Regions & Role
    public QuizResponseByRegionDTO getQuizResponseByRegion(String surveyId) {
        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(surveyId);
        QuizSurveyModel quiz = quizSurveyRepo.findById(surveyId)
                .orElseThrow(() -> new RuntimeException("Survey not found"));

        List<UserModel> invitedUsers = userRepository.findAllById(quiz.getTargetedUsers());

        // Map for region counts
        Map<String, Integer> byRegion = new HashMap<>();
        for (UserModel user : invitedUsers) {
            String region = safeString(user.getRegion());
            boolean responded = responses.stream().anyMatch(r -> r.getUserId().equals(user.getId()));
            if (responded) {
                byRegion.put(region, byRegion.getOrDefault(region, 0) + 1);
            }
        }

        // Map for role counts
        Map<String, Integer> byRole = new HashMap<>();
        for (UserModel user : invitedUsers) {
            String role = safeString(user.getPosition()); // Or user.getRole() if exists
            boolean responded = responses.stream().anyMatch(r -> r.getUserId().equals(user.getId()));
            if (responded) {
                byRole.put(role, byRole.getOrDefault(role, 0) + 1);
            }
        }

        return QuizResponseByRegionDTO.builder()
                .title("Segmentation")
                .byRegion(byRegion)
                .byRole(byRole)
                .build();
    }

    // ========== HELPERS ==========
    private <T> String safeString(String value) {
        return value != null && !value.trim().isEmpty() ? value : "Unknown";
    }

    private double calculateRate(int responded, int invited) {
        return invited == 0 ? 0.0 : Math.round((responded * 10000.0 / invited)) / 100.0;
    }
}
