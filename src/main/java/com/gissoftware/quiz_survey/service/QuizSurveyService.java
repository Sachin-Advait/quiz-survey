package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.Utils.UserDataFieldConstants;
import com.gissoftware.quiz_survey.controller.QuizSurveySocketController;
import com.gissoftware.quiz_survey.dto.QuizScoreSummaryDTO;
import com.gissoftware.quiz_survey.dto.QuizSurveyDTO;
import com.gissoftware.quiz_survey.dto.QuizzesSurveysDTO;
import com.gissoftware.quiz_survey.mapper.QuizSurveyMapper;
import com.gissoftware.quiz_survey.model.AnnouncementMode;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.VisibilityType;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class QuizSurveyService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final UserRepository userRepository;
    private final QuizSurveySocketController quizSurveySocketController;
    private final QuizSurveyMapper quizSurveyMapper;
    private final ResponseRepo responseRepo;

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
                .maxRetake(quiz.getMaxRetake())
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

        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid username"));

        quizzes = quizSurveyRepo.findAll().stream()
                .filter(quiz -> quiz.getTargetedUsers() != null &&
                        quiz.getTargetedUsers().contains(userId))
                .toList();

        return quizzes.stream()
                .map(quiz -> quizSurveyMapper.mapToDtoWithUser(quiz, userId))
                .toList();
    }

    // Create Quiz & Survey
    public QuizSurveyModel createQuizSurvey(QuizSurveyModel model) {

        if (model.getVisibilityType() == null) {
            throw new IllegalArgumentException("Visibility not defined!");
        }

        if (model.getVisibilityType() == VisibilityType.PRIVATE && !model.getUserDataDisplayFields().isEmpty()) {
            List<String> filteredFields = model.getUserDataDisplayFields().stream()
                    .filter(UserDataFieldConstants.ALLOWED_FIELDS::contains)
                    .distinct()
                    .toList();

            model.setUserDataDisplayFields(filteredFields);
        } else if (model.getVisibilityType() == VisibilityType.PRIVATE) {
            throw new IllegalArgumentException("User field are empty");
        }

        model.setIsAnnounced(model.getAnnouncementMode() == AnnouncementMode.IMMEDIATE);

        QuizSurveyModel quizSurveyModel = quizSurveyRepo.save(model);

        quizSurveySocketController.pushNewSurvey(
                quizSurveyModel.getId(),
                quizSurveyModel.getIsMandatory(),
                quizSurveyModel.getTargetedUsers()
        );
        return quizSurveyModel;
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
        if (model.getMaxRetake() != null) existing.setMaxRetake(model.getMaxRetake());

        return quizSurveyRepo.save(existing);
    }

    // Delete Quiz & Survey by ID
    public void deleteQuizSurvey(String id) {
        quizSurveyRepo.deleteById(id);
    }

    // Get Quiz Score Summary
    public QuizScoreSummaryDTO quizScoreSummary(String quizSurveyId) {
        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);
        if (responses.isEmpty()) {
            throw new IllegalArgumentException("No responses found for quiz/survey: " + quizSurveyId);
        }

        // ✅ Pick only the best attempt per user
        Map<String, ResponseModel> bestAttempts = responses.stream()
                .filter(r -> r.getScore() != null)
                .collect(Collectors.toMap(
                        ResponseModel::getUserId,
                        r -> r,
                        (r1, r2) -> r1.getScore() >= r2.getScore() ? r1 : r2 // keep higher score
                ));

        Collection<ResponseModel> uniqueResponses = bestAttempts.values();

        int totalAttempts = uniqueResponses.size();
        int totalScore = 0, highestScore = 0, maxScore = 0;

        for (ResponseModel r : uniqueResponses) {
            if (r.getScore() != null) {
                totalScore += r.getScore();
                highestScore = Math.max(highestScore, r.getScore());
            }
            if (r.getMaxScore() != null) {
                maxScore = Math.max(maxScore, r.getMaxScore());
            }
        }

        double avgScore = totalAttempts > 0 ? (double) totalScore / totalAttempts : 0.0;

        // ✅ Get top 3 scorers (unique users only)
        List<Map<String, Object>> topScorers = uniqueResponses.stream()
                .filter(r -> r.getScore() != null)
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .limit(3)
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("username", r.getUsername());
                    map.put("userId", r.getUserId());
                    map.put("score", r.getScore());
                    map.put("maxScore", r.getMaxScore());
                    return map;
                })
                .collect(Collectors.toList());

        return new QuizScoreSummaryDTO(totalAttempts, avgScore, highestScore, maxScore, topScorers);
    }
}
