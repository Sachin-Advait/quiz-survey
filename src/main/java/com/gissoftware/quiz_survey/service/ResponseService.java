package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.QuizScoreSummaryDTO;
import com.gissoftware.quiz_survey.dto.SurveyResultDTO;
import com.gissoftware.quiz_survey.dto.SurveySubmissionDTO;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.SurveyDefinition;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResponseService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final ResponseRepo responseRepo;
    private final UserRepository userRepository;

    @Transactional
    public ResponseModel storeResponse(String quizSurveyId, SurveySubmissionDTO request) {

        // Fetch quiz/survey
        QuizSurveyModel qs = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz or survey not found"));

        // ✅ Check if the user has already submitted a response
        boolean alreadySubmitted = responseRepo.findByQuizSurveyIdAndUserId(quizSurveyId,
                request.getUserId()).isPresent();

        if (alreadySubmitted) {
            throw new IllegalStateException("You have already submitted this quiz/survey.");
        }

        // Handle response
        return switch (qs.getType().toLowerCase()) {
            case "survey" -> handleSurveyResponse(qs, request.getUserId(), request.getAnswers());
            case "quiz" -> handleQuizResponse(qs, request.getUserId(), request.getAnswers());
            default -> throw new IllegalArgumentException("Unsupported type: " + qs.getType());
        };
    }


    private ResponseModel handleQuizResponse(QuizSurveyModel quiz, String userId, Map<String, Object> userAnswers) {
        ScoringUtil.ScoringResult result = ScoringUtil.score(userAnswers, quiz.getAnswerKey());

        return responseRepo.save(ResponseModel.builder()
                .quizSurveyId(quiz.getId())
                .userId(userId)
                .answers(userAnswers)
                .score(result.score())
                .maxScore(result.max())
                .build());
    }

    private ResponseModel handleSurveyResponse(QuizSurveyModel survey, String userId, Map<String, Object> userAnswers) {
        return responseRepo.save(ResponseModel.builder()
                .quizSurveyId(survey.getId())
                .userId(userId)
                .answers(userAnswers)
                .score(null)
                .maxScore(null)
                .build());
    }


    public QuizScoreSummaryDTO getScoreSummary(String quizSurveyId) {
        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);
        if (responses.isEmpty()) {
            throw new IllegalArgumentException("No responses found for quiz/survey: " + quizSurveyId);
        }

        int totalAttempts = responses.size();
        int totalScore = 0, highestScore = 0, maxScore = 0;

        for (ResponseModel r : responses) {
            if (r.getScore() != null) {
                totalScore += r.getScore();
                highestScore = Math.max(highestScore, r.getScore());
            }
            if (r.getMaxScore() != null) {
                maxScore = Math.max(maxScore, r.getMaxScore());
            }
        }

        double avgScore = (double) totalScore / totalAttempts;

        return new QuizScoreSummaryDTO(totalAttempts, avgScore, highestScore, maxScore);
    }

    public List<ResponseModel> getResponsesByUserId(String userId) {
        return responseRepo.findByUserId(userId);
    }

    public List<ResponseModel> getResponsesByQuizSurveyId(String quizSurveyId) {
        return responseRepo.findByQuizSurveyId(quizSurveyId);
    }

    public ResponseModel getResponseById(String id) {
        return responseRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Response not found with id: " + id));
    }

    public List<SurveyResultDTO> getSurveyResults(String quizSurveyId) {
        // Fetch survey
        QuizSurveyModel survey = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));

        // Extract questions and choices from definitionJson
        SurveyDefinition definition = survey.getDefinitionJson(); // assuming definitionJson is already deserialized
        Map<String, Map<String, Integer>> counts = new HashMap<>();

        for (SurveyDefinition.Page page : definition.getPages()) {
            for (SurveyDefinition.Element element : page.getElements()) {
                if (element.getChoices() != null) {
                    Map<String, Integer> choiceCounts = new HashMap<>();
                    for (String choice : element.getChoices()) {
                        choiceCounts.put(choice, 0); // initialize with 0
                    }
                    counts.put(element.getName(), choiceCounts);
                }
            }
        }

        // Get responses
        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);
        if (responses.isEmpty()) {
            throw new IllegalArgumentException("No responses found for this survey.");
        }

        int totalResponses = responses.size();

        // Aggregate answers
        for (ResponseModel response : responses) {
            Map<String, Object> answers = response.getAnswers();
            for (Map.Entry<String, Object> entry : answers.entrySet()) {
                String questionKey = entry.getKey(); // like "q1"
                String answer = String.valueOf(entry.getValue());

                Map<String, Integer> optionCounts = counts.get(questionKey);
                if (optionCounts != null && optionCounts.containsKey(answer)) {
                    optionCounts.put(answer, optionCounts.get(answer) + 1);
                }
            }
        }

        // Build result list
        List<SurveyResultDTO> results = new ArrayList<>();
        for (SurveyDefinition.Page page : definition.getPages()) {
            for (SurveyDefinition.Element element : page.getElements()) {
                if (element.getChoices() != null) {
                    String questionTitle = element.getTitle(); // e.g., "What is AI?"
                    String questionKey = element.getName();    // e.g., "q1"
                    Map<String, Integer> countMap = counts.getOrDefault(questionKey, new HashMap<>());
                    Map<String, Integer> percentageMap = new HashMap<>();

                    for (String choice : element.getChoices()) {
                        int count = countMap.getOrDefault(choice, 0);
                        int percentage = (int) Math.round((count * 100.0) / totalResponses);
                        percentageMap.put(choice, percentage);
                    }

                    results.add(new SurveyResultDTO(questionTitle, percentageMap));
                }
            }
        }

        return results;
    }

}
