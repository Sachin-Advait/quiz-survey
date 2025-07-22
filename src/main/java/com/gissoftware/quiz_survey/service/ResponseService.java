package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.QuizResultDTO;
import com.gissoftware.quiz_survey.dto.QuizScoreSummaryDTO;
import com.gissoftware.quiz_survey.dto.SurveyResultDTO;
import com.gissoftware.quiz_survey.dto.SurveySubmissionRequest;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.SurveyDefinition;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResponseService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final ResponseRepo responseRepo;
    private final UserRepository userRepository;

    // Store Quiz & Survey Responses
    @Transactional
    public ResponseModel storeResponse(String quizSurveyId, SurveySubmissionRequest request) {
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Invalid username"));

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
            case "survey" -> handleSurveyResponse(qs, request);
            case "quiz" -> handleQuizResponse(qs, request);
            default -> throw new IllegalArgumentException("Unsupported type: " + qs.getType());
        };
    }


    private ResponseModel handleQuizResponse(QuizSurveyModel quiz, SurveySubmissionRequest request) {
        ScoringUtil.ScoringResult result = ScoringUtil.score(request.getAnswers(), quiz.getAnswerKey());

        UserModel user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Invalid userId"));

        return responseRepo.save(ResponseModel.builder()
                .quizSurveyId(quiz.getId())
                .userId(request.getUserId())
                .username(user.getUsername())
                .answers(request.getAnswers())
                .score(result.score())
                .maxScore(result.max())
                .finishTime(request.getFinishTime())
                .build());
    }

    private ResponseModel handleSurveyResponse(QuizSurveyModel survey, SurveySubmissionRequest request) {
        UserModel user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Invalid userId"));

        return responseRepo.save(ResponseModel.builder()
                .quizSurveyId(survey.getId())
                .userId(request.getUserId())
                .username(user.getUsername())
                .answers(request.getAnswers())
                .score(null)
                .maxScore(null)
                .finishTime(null)
                .build());
    }


    // Get Quiz Score Summary
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

        // Get top 3 scorers
        List<Map<String, Object>> topScorers = responses.stream()
                .filter(r -> r.getScore() != null)
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .limit(3)
                .map(r -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("username", r.getUsername()); // assuming getUsername() exists
                    map.put("score", r.getScore());
                    map.put("maxScore", r.getMaxScore());
                    return map;
                })
                .collect(Collectors.toList());

        return new QuizScoreSummaryDTO(totalAttempts, avgScore, highestScore, maxScore, topScorers);
    }


    // Get All Responses by User ID
    public List<ResponseModel> getAllResponsesByUserId(String userId) {
        return responseRepo.findByUserId(userId);
    }

    // Get Quiz Result by User ID Common Function
    private QuizResultDTO buildQuizResultDTO(ResponseModel response, QuizSurveyModel quizSurvey) {
        SurveyDefinition definition = quizSurvey.getDefinitionJson();
        Map<String, Object> answerKey = quizSurvey.getAnswerKey();
        Map<String, Object> selectedAnswers = response.getAnswers();

        Map<String, QuizResultDTO.QuestionAnswerDTO> formattedAnswers = new LinkedHashMap<>();

        for (SurveyDefinition.Page page : definition.getPages()) {
            for (SurveyDefinition.Element q : page.getElements()) {
                String questionText = q.getTitle();
                List<String> choices = q.getChoices();
                String questionId = q.getName();

                List<QuizResultDTO.OptionDTO> formattedOptions = new ArrayList<>();
                char label = 'A';

                for (String choice : choices) {
                    String labelStr = String.valueOf(label++);
                    boolean isCorrect = false;

                    Object correct = answerKey.get(questionId);
                    if (correct instanceof String) {
                        isCorrect = labelStr.equalsIgnoreCase((String) correct);
                    } else if (correct instanceof List) {
                        isCorrect = ((List<?>) correct).contains(labelStr);
                    }

                    formattedOptions.add(QuizResultDTO.OptionDTO.builder()
                            .text(choice)
                            .isCorrect(isCorrect)
                            .build());
                }

                // Extract selected options
                Object selected = selectedAnswers.get(questionId);
                List<String> selectedOptionLabels = new ArrayList<>();
                if (selected instanceof String) {
                    selectedOptionLabels.add((String) selected);
                } else if (selected instanceof List) {
                    selectedOptionLabels = ((List<?>) selected).stream()
                            .map(String::valueOf)
                            .toList();
                }

                formattedAnswers.put(questionText,
                        QuizResultDTO.QuestionAnswerDTO.builder()
                                .options(formattedOptions)
                                .selectedOptions(selectedOptionLabels)
                                .build());
            }
        }

        return QuizResultDTO.builder()
                .username(response.getUsername())
                .score(response.getScore())
                .maxScore(response.getMaxScore())
                .submittedAt(response.getSubmittedAt())
                .answers(formattedAnswers)
                .build();
    }

    // Get Quiz Result by User ID
    public QuizResultDTO getQuizResultByUserId(String quizSurveyId, String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid userId"));

        ResponseModel response = responseRepo.findByQuizSurveyIdAndUserId(quizSurveyId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Response not found for this user and quiz."));

        QuizSurveyModel quizSurvey = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new RuntimeException("Quiz survey not found"));

        return buildQuizResultDTO(response, quizSurvey);
    }

    // Get Quiz Result Admin
    public List<QuizResultDTO> getQuizResultsAdmin(String quizSurveyId) {
        QuizSurveyModel quizSurvey = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new RuntimeException("Quiz survey not found"));

        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);

        return responses.stream()
                .map(response -> buildQuizResultDTO(response, quizSurvey))
                .toList();
    }

    // Get Survey Result
    public List<SurveyResultDTO> getSurveyResults(String quizSurveyId) {
        // Fetch survey
        QuizSurveyModel survey = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));

        // Extract survey definition
        SurveyDefinition definition = survey.getDefinitionJson();
        Map<String, Map<String, Integer>> counts = new HashMap<>();
        Map<String, List<Integer>> ratingResponses = new HashMap<>();
        Map<String, Map<String, Integer>> rankingScores = new HashMap<>();
        Map<String, Integer> rankingRespondentCounts = new HashMap<>();

        // Initialize maps
        for (SurveyDefinition.Page page : definition.getPages()) {
            for (SurveyDefinition.Element element : page.getElements()) {
                String key = element.getName();
                String type = element.getType().toLowerCase();

                if ("ranking".equals(type) && element.getChoices() != null) {
                    Map<String, Integer> scoreMap = new HashMap<>();
                    for (String choice : element.getChoices()) {
                        scoreMap.put(choice, 0);
                    }
                    rankingScores.put(key, scoreMap);
                    rankingRespondentCounts.put(key, 0);
                } else if (element.getChoices() != null) {
                    Map<String, Integer> choiceCounts = new HashMap<>();
                    for (String choice : element.getChoices()) {
                        choiceCounts.put(choice, 0);
                    }
                    counts.put(key, choiceCounts);
                } else if ("rating".equals(type)) {
                    ratingResponses.put(key, new ArrayList<>());
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
                String key = entry.getKey();
                Object value = entry.getValue();

                if (counts.containsKey(key)) {
                    if (value instanceof List<?> list) {
                        for (Object val : list) {
                            String choice = String.valueOf(val);
                            counts.get(key).computeIfPresent(choice, (k, v) -> v + 1);
                        }
                    } else {
                        String choice = String.valueOf(value);
                        counts.get(key).computeIfPresent(choice, (k, v) -> v + 1);
                    }
                } else if (ratingResponses.containsKey(key)) {
                    try {
                        int rating = (int) Double.parseDouble(value.toString());
                        ratingResponses.get(key).add(rating);
                    } catch (Exception e) {
                        // Optional: log or ignore
                    }

                } else if (rankingScores.containsKey(key) && value instanceof List<?> rankingList) {
                    int rank = 1;
                    for (Object item : rankingList) {
                        String choice = String.valueOf(item);
                        int finalRank = rank;
                        rankingScores.get(key).computeIfPresent(choice, (k, v) -> v + finalRank);
                        rank++;
                    }
                    rankingRespondentCounts.put(key, rankingRespondentCounts.getOrDefault(key, 0) + 1);
                }
            }
        }

        // Build final result
        List<SurveyResultDTO> results = new ArrayList<>();
        for (SurveyDefinition.Page page : definition.getPages()) {
            for (SurveyDefinition.Element element : page.getElements()) {
                String key = element.getName();
                String title = element.getTitle();
                String type = element.getType().toLowerCase();

                if (counts.containsKey(key)) {
                    Map<String, Integer> countMap = counts.get(key);
                    Map<String, Object> percentageMap = new HashMap<>();
                    for (String choice : countMap.keySet()) {
                        int count = countMap.getOrDefault(choice, 0);
                        int percentage = (int) Math.round((count * 100.0) / totalResponses);
                        percentageMap.put(choice, percentage);
                    }
                    results.add(new SurveyResultDTO(title, "choice", percentageMap));
                } else if (ratingResponses.containsKey(key)) {
                    List<Integer> ratings = ratingResponses.get(key);
                    double average = ratings.stream().mapToInt(i -> i).average().orElse(0.0);
                    Map<String, Object> ratingResult = new HashMap<>();
                    ratingResult.put("averageRating", average);
                    ratingResult.put("responseCount", ratings.size());

                    results.add(new SurveyResultDTO(title, "rating", ratingResult));
                } else if (rankingScores.containsKey(key)) {
                    Map<String, Integer> scoreMap = rankingScores.get(key);
                    int respondentCount = rankingRespondentCounts.getOrDefault(key, 1);

                    Map<String, Object> avgRankMap = new HashMap<>();
                    for (Map.Entry<String, Integer> entry : scoreMap.entrySet()) {
                        double avgRank = (double) entry.getValue() / respondentCount;
                        avgRankMap.put(entry.getKey(), avgRank);
                    }

                    results.add(new SurveyResultDTO(title, "ranking", avgRankMap));
                }
            }
        }

        return results;
    }
}
