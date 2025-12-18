package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.UserSingleScoreDTO;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.SurveyDefinition;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserScoreService {

    // ===== WEIGHTS (configurable later) =====
    private static final double QUIZ_WEIGHT = 0.6;
    private static final double SURVEY_PARTICIPATION_WEIGHT = 0.2;
    private static final double SATISFACTION_WEIGHT = 0.2;
    private final ResponseRepo responseRepo;
    private final QuizSurveyRepository quizSurveyRepo;
    private final UserRepository userRepository;

    public UserSingleScoreDTO calculateUserScore(String userId) {

        // ✅ Validate user
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid userId"));

        // Fetch all responses of user
        List<ResponseModel> responses = responseRepo.findByUserId(userId);

        double quizScore = calculateQuizScore(responses);
        double surveyParticipation = calculateSurveyParticipation(userId);
        double satisfactionScore = calculateSatisfactionScore(responses);

        double finalScore =
                (quizScore * QUIZ_WEIGHT)
                        + (surveyParticipation * SURVEY_PARTICIPATION_WEIGHT)
                        + (satisfactionScore * SATISFACTION_WEIGHT);

        return new UserSingleScoreDTO(
                userId,
                round(quizScore),
                round(surveyParticipation),
                round(satisfactionScore),
                round(finalScore)
        );
    }

    // ================= QUIZ SCORE =================
    private double calculateQuizScore(List<ResponseModel> responses) {

        // Group by quizId → pick best attempt
        Map<String, ResponseModel> bestAttempts =
                responses.stream()
                        .filter(r -> r.getScore() != null)
                        .collect(Collectors.toMap(
                                ResponseModel::getQuizSurveyId,
                                r -> r,
                                (r1, r2) -> r1.getScore() >= r2.getScore() ? r1 : r2
                        ));

        if (bestAttempts.isEmpty()) return 0.0;

        double totalPercentage = 0;

        for (ResponseModel r : bestAttempts.values()) {
            if (r.getMaxScore() != null && r.getMaxScore() > 0) {
                totalPercentage += (r.getScore() * 100.0) / r.getMaxScore();
            }
        }

        return totalPercentage / bestAttempts.size();
    }

    // ================= SURVEY PARTICIPATION =================
    private double calculateSurveyParticipation(String userId) {

        List<QuizSurveyModel> assignedSurveys =
                quizSurveyRepo.findByTypeIgnoreCase("survey");

        if (assignedSurveys.isEmpty()) return 0.0;

        long assignedCount =
                assignedSurveys.stream()
                        .filter(s -> s.getTargetedUsers().contains(userId))
                        .count();

        if (assignedCount == 0) return 0.0;

        Set<String> respondedSurveyIds =
                responseRepo.findByUserId(userId).stream()
                        .map(ResponseModel::getQuizSurveyId)
                        .collect(Collectors.toSet());

        long participatedCount =
                assignedSurveys.stream()
                        .filter(s -> respondedSurveyIds.contains(s.getId()))
                        .count();

        return (participatedCount * 100.0) / assignedCount;
    }

    // ================= SATISFACTION SCORE =================
    private double calculateSatisfactionScore(List<ResponseModel> responses) {

        List<Integer> ratings = new ArrayList<>();

        for (ResponseModel response : responses) {
            QuizSurveyModel survey =
                    quizSurveyRepo.findById(response.getQuizSurveyId()).orElse(null);

            if (survey == null || !"survey".equalsIgnoreCase(survey.getType())) continue;

            List<SurveyDefinition.Element> ratingQuestions =
                    survey.getDefinitionJson().getPages().stream()
                            .flatMap(p -> p.getElements().stream())
                            .filter(e -> "rating".equalsIgnoreCase(e.getType()))
                            .toList();

            for (SurveyDefinition.Element q : ratingQuestions) {
                Object val = response.getAnswers().get(q.getName());
                if (val instanceof Number) {
                    ratings.add(((Number) val).intValue());
                }
            }
        }

        if (ratings.isEmpty()) return 0.0;

        double avgRating = ratings.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        // Normalize 1–5 → 0–100
        return (avgRating / 5.0) * 100.0;
    }

    // ================= HELPERS =================
    private double round(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
