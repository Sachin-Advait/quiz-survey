package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.controller.QuizSurveySocketController;
import com.gissoftware.quiz_survey.dto.QuizCompletionStatsDTO;
import com.gissoftware.quiz_survey.dto.QuizInsightsDTO;
import com.gissoftware.quiz_survey.dto.QuizSurveyDTO;
import com.gissoftware.quiz_survey.dto.QuizzesSurveysDTO;
import com.gissoftware.quiz_survey.mapper.QuizSurveyMapper;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.SurveyDefinition;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizSurveyService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final ResponseRepo responseRepo;
    private final UserRepository userRepository;
    private final QuizSurveySocketController quizSurveySocketController;
    private final QuizSurveyMapper quizSurveyMapper;

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

    // Create Quiz & Survey
    public QuizSurveyModel createQuizSurvey(QuizSurveyModel model) {

        QuizSurveyModel quizSurveyModel = quizSurveyRepo.save(model);
        quizSurveySocketController.pushNewSurvey(
                quizSurveyModel.getId(),
                quizSurveyModel.getIsMandatory(),
                quizSurveyModel.getTargetedUsers()
        );
        return quizSurveyModel;
    }

    // Get Quiz & Survey By ID
    public QuizSurveyDTO getQuizSurvey(String id) {
        QuizSurveyModel quiz = quizSurveyRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz or survey not found"));

        return QuizSurveyDTO.builder()
                .id(quiz.getId())
                .type(quiz.getType())
                .title(quiz.getTitle())
                .definitionJson(quiz.getDefinitionJson())
                .quizDuration(quiz.getQuizDuration())
                .maxScore(quiz.getMaxScore())
                .answerKey(quiz.getAnswerKey())
                .createdAt(quiz.getCreatedAt())
                .build();
    }

    // Get All Quizzes and Surveys
    public List<QuizzesSurveysDTO> getQuizzesSurveys(String userId) {
        List<QuizSurveyModel> quizzes;

        if (userId == null) {
            quizzes = quizSurveyRepo.findAll();

            return quizzes.stream()
                    .map(quizSurveyMapper::mapToDtoWithoutUser)
                    .toList();
        }

        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid username"));

        String username = user.getUsername();

        quizzes = quizSurveyRepo.findAll().stream()
                .filter(quiz -> quiz.getTargetedUsers() != null &&
                        quiz.getTargetedUsers().contains(username))
                .toList();

        return quizzes.stream()
                .map(quiz -> quizSurveyMapper.mapToDtoWithUser(quiz, userId))
                .toList();
    }

    // Update Quiz & Survey by ID
    public QuizSurveyModel updateQuizSurvey(QuizSurveyModel model) {
        QuizSurveyModel existing = quizSurveyRepo.findById(model.getId())
                .orElseThrow(() -> new IllegalArgumentException("Quiz/Survey not found"));

        if (model.getTitle() != null) existing.setTitle(model.getTitle());
        if (model.getType() != null) existing.setType(model.getType());
        if (model.getDefinitionJson() != null) existing.setDefinitionJson(model.getDefinitionJson());
        if (model.getAnswerKey() != null) existing.setAnswerKey(model.getAnswerKey());
        if (model.getMaxScore() != null) existing.setMaxScore(model.getMaxScore());
        if (model.getStatus() != null) existing.setStatus(model.getStatus());
        if (model.getQuizTotalDuration() != null) existing.setQuizTotalDuration(model.getQuizTotalDuration());
        if (model.getQuizDuration() != null) existing.setQuizDuration(model.getQuizDuration());
        if (model.getIsAnnounced() != null) existing.setIsAnnounced(model.getIsAnnounced());
        if (model.getIsMandatory() != null) existing.setIsMandatory(model.getIsMandatory());
        if (!model.getTargetedUsers().isEmpty()) existing.setTargetedUsers(model.getTargetedUsers());

        return quizSurveyRepo.save(existing);
    }

    // Delete Quiz & Survey by ID
    public void deleteQuizSurvey(String id) {
        quizSurveyRepo.deleteById(id);
    }

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

                boolean isCorrect = isCorrect(correctAnswer, userAnswer);
                if (!isCorrect) {
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
                .averageScore(formattedAverage)
                .passRate(100.0 * passCount / totalUsers)
                .failRate(100.0 * (totalUsers - passCount) / totalUsers)
                .topScorer(new QuizInsightsDTO.ScorerDTO(top.getUsername(), top.getScore()))
                .lowestScorer(new QuizInsightsDTO.ScorerDTO(low.getUsername(), low.getScore()))
                .mostIncorrectQuestions(mostIncorrectQuestions)
                .build();
    }


    public QuizCompletionStatsDTO getQuizCompletionStats(String quizId) {
        var quiz = quizSurveyRepo.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        List<String> targetedUsers = quiz.getTargetedUsers();
        int totalAssigned = targetedUsers.size();

        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizId);
        List<String> completedUserIds = responses.stream()
                .map(ResponseModel::getUsername)
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
}
