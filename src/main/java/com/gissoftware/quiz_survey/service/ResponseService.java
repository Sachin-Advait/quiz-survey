package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.Utils.ScoringUtil;
import com.gissoftware.quiz_survey.dto.*;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.SurveyDefinition;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResponseService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final ResponseRepo responseRepo;
    private final UserRepository userRepository;

    // Store Quiz & Survey Responses
//    @Transactional
    public ResponseModel storeResponse(String quizSurveyId, SurveySubmissionRequest request) {
        userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Invalid username"));

        // Fetch quiz/survey
        QuizSurveyModel qs = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz or survey not found"));

        // ✅ Check if the user has already submitted a response
        if (qs.getType().equalsIgnoreCase("survey")) {
            List<ResponseModel> existingResponses = responseRepo.findByQuizSurveyIdAndUserId(quizSurveyId, request.getUserId());
            if (!existingResponses.isEmpty()) {
                throw new IllegalStateException("You have already submitted this survey.");
            }
        }

        if (qs.getType().equalsIgnoreCase("quiz")) {
            List<ResponseModel> existingResponses = responseRepo.findByQuizSurveyIdAndUserId(quizSurveyId,
                    request.getUserId());
            if (existingResponses.size() >= qs.getMaxRetake()) {
                throw new IllegalStateException("You have reach the limit on this quiz.");
            }
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
                .finishTime(request.getFinishTime())
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


    // Get Quiz Result by User ID
    public QuizResultDTO getQuizResultByUserId(String quizSurveyId, String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid userId"));

        List<ResponseModel> responses = responseRepo.findByQuizSurveyIdAndUserId(quizSurveyId, userId);
        if (responses.isEmpty()) {
            throw new IllegalArgumentException("Response not found for this user and quiz.");
        }

        ResponseModel highestScoreInQuizResult = responses.stream()
                .max(Comparator.comparing(ResponseModel::getScore, Comparator.nullsLast(Integer::compareTo)))
                .orElseThrow(() -> new IllegalArgumentException("No responses found."));

        QuizSurveyModel quizSurvey = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new RuntimeException("Quiz survey not found"));

        SurveyDefinition definition = quizSurvey.getDefinitionJson();
        Map<String, Object> answerKey = quizSurvey.getAnswerKey();

        Map<String, Object> selectedAnswers = highestScoreInQuizResult.getAnswers();
        Map<String, QuizResultDTO.QuestionAnswerDTO> formattedAnswers = new LinkedHashMap<>();

        for (SurveyDefinition.Page page : definition.getPages()) {
            for (SurveyDefinition.Element q : page.getElements()) {
                String questionText = q.getTitle();
                List<String> choices = q.getChoices();
                String questionId = q.getName();

                List<QuizResultDTO.OptionDTO> formattedOptions = new ArrayList<>();

                if (choices != null) {
                    for (String choice : choices) {
                        boolean isCorrect = false;
                        Object correct = answerKey.get(questionId);

                        if (correct instanceof String) {
                            isCorrect = choice.equals(correct);
                        } else if (correct instanceof List) {
                            isCorrect = ((List<?>) correct).contains(choice);
                        }

                        formattedOptions.add(QuizResultDTO.OptionDTO.builder()
                                .text(choice)
                                .isCorrect(isCorrect)
                                .build());
                    }
                }

                Object selected = selectedAnswers.get(questionId);
                Object selectedOptions = null;

                if (selected instanceof List<?> selectedList) {
                    if (selectedList.size() == 1) {
                        selectedOptions = String.valueOf(selectedList.get(0));
                    } else if (!selectedList.isEmpty()) {
                        selectedOptions = selectedList.stream()
                                .map(String::valueOf)
                                .toList();
                    }
                } else if (selected != null) {
                    selectedOptions = selected.toString();
                }

                formattedAnswers.put(questionText,
                        QuizResultDTO.QuestionAnswerDTO.builder()
                                .choices(formattedOptions)
                                .type(q.getType())
                                .correctAnswer(Objects.equals(q.getType(), "text") && q.getCorrectAnswer() != null
                                        ? q.getCorrectAnswer().toString()
                                        : null)
                                .selectedOptions(selectedOptions)
                                .build());
            }
        }

        return QuizResultDTO.builder()
                .id(highestScoreInQuizResult.getId())
                .username(highestScoreInQuizResult.getUsername())
                .score(highestScoreInQuizResult.getScore())
                .maxScore(highestScoreInQuizResult.getMaxScore())
                .submittedAt(highestScoreInQuizResult.getSubmittedAt())
                .answers(formattedAnswers)
                .finishTime(highestScoreInQuizResult.getFinishTime())
                .build();
    }


    // Get Quiz Result Admin
    public List<QuizResultAdminDTO> getQuizResultsAdmin(String quizSurveyId) {
        QuizSurveyModel quizSurvey = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new RuntimeException("Quiz survey not found"));

        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);

        SurveyDefinition definition = quizSurvey.getDefinitionJson();
        Map<String, Object> answerKey = quizSurvey.getAnswerKey();

        // Group responses by userId
        Map<String, List<ResponseModel>> responsesByUser = responses.stream()
                .collect(Collectors.groupingBy(ResponseModel::getUserId));

        List<QuizResultAdminDTO> userResults = new ArrayList<>();

        for (Map.Entry<String, List<ResponseModel>> entry : responsesByUser.entrySet()) {
            String userId = entry.getKey();
            List<ResponseModel> userAttempts = entry.getValue();

            List<QuizResultDTO> attemptsDTO = userAttempts.stream().map(attempt -> {
                Map<String, QuizResultDTO.QuestionAnswerDTO> formattedAnswers = new LinkedHashMap<>();

                Map<String, Object> selectedAnswers = attempt.getAnswers();

                for (SurveyDefinition.Page page : definition.getPages()) {
                    for (SurveyDefinition.Element q : page.getElements()) {
                        String questionText = q.getTitle();
                        List<String> choices = q.getChoices();
                        String questionId = q.getName();

                        List<QuizResultDTO.OptionDTO> formattedOptions = new ArrayList<>();

                        if (choices != null) {
                            for (String choice : choices) {
                                boolean isCorrect = false;
                                Object correct = answerKey.get(questionId);

                                if (correct instanceof String) {
                                    isCorrect = choice.equals(correct);
                                } else if (correct instanceof List) {
                                    isCorrect = ((List<?>) correct).contains(choice);
                                }

                                formattedOptions.add(QuizResultDTO.OptionDTO.builder()
                                        .text(choice)
                                        .isCorrect(isCorrect)
                                        .build());
                            }
                        }

                        Object selected = selectedAnswers.get(questionId);
                        Object selectedOptions = null;

                        if (selected instanceof List<?> selectedList) {
                            if (selectedList.size() == 1) {
                                selectedOptions = String.valueOf(selectedList.get(0));
                            } else if (!selectedList.isEmpty()) {
                                selectedOptions = selectedList.stream()
                                        .map(String::valueOf)
                                        .toList();
                            }
                        } else if (selected != null) {
                            selectedOptions = selected.toString();
                        }

                        formattedAnswers.put(questionText, QuizResultDTO.QuestionAnswerDTO.builder()
                                .choices(formattedOptions)
                                .type(q.getType())
                                .correctAnswer(Objects.equals(q.getType(), "text") && q.getCorrectAnswer() != null
                                        ? q.getCorrectAnswer().toString()
                                        : null)
                                .selectedOptions(selectedOptions)
                                .build());
                    }
                }

                return QuizResultDTO.builder()
                        .id(attempt.getId())
                        .username(attempt.getUsername())
                        .score(attempt.getScore())
                        .maxScore(attempt.getMaxScore())
                        .submittedAt(attempt.getSubmittedAt())
                        .answers(formattedAnswers)
                        .finishTime(attempt.getFinishTime())
                        .build();
            }).toList();

            userResults.add(QuizResultAdminDTO.builder()
                    .id(userId)
                    .username(userAttempts.get(0).getUsername())
                    .attempts(attemptsDTO)
                    .build());
        }

        return userResults;
    }


    // Get Survey Result
    public List<SurveyResultDTO> getSurveyResultsAdmin(String quizSurveyId) {
        QuizSurveyModel survey = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));

        SurveyDefinition definition = survey.getDefinitionJson();
        Map<String, Map<String, Integer>> counts = new HashMap<>();
        Map<String, List<Integer>> ratingResponses = new HashMap<>();

        // ✅ Initialize counts and ratingResponses
        for (SurveyDefinition.Page page : definition.getPages()) {
            for (SurveyDefinition.Element element : page.getElements()) {
                String key = element.getName();
                String type = element.getType().toLowerCase();

                if ("rating".equals(type)) {
                    ratingResponses.put(key, new ArrayList<>());
                } else if (List.of("radiogroup", "checkbox", "dropdown").contains(type) && element.getChoices() != null) {
                    Map<String, Integer> choiceCounts = new HashMap<>();
                    for (String choice : element.getChoices()) {
                        choiceCounts.put(choice, 0);
                    }
                    counts.put(key, choiceCounts);
                }
            }
        }

        // ✅ Get responses
        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);
        if (responses.isEmpty()) {
            throw new IllegalArgumentException("No responses found for this survey.");
        }
        int totalResponses = responses.size();

        // ✅ Aggregate answers
        for (ResponseModel response : responses) {
            Map<String, Object> answers = response.getAnswers();
            if (answers == null) continue;
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
                }
            }
        }

        // ✅ Build final result
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
                    results.add(new SurveyResultDTO(title, type, percentageMap));
                } else if (ratingResponses.containsKey(key)) {
                    List<Integer> ratings = ratingResponses.get(key);
                    double average = ratings.stream().mapToInt(i -> i).average().orElse(0.0);
                    Map<String, Object> ratingResult = new HashMap<>();
                    ratingResult.put("averageRating", average);
                    ratingResult.put("responseCount", ratings.size());

                    results.add(new SurveyResultDTO(title, "rating", ratingResult));
                } else if ("boolean".equals(type)) {
                    int yesCount = 0;
                    int noCount = 0;

                    for (ResponseModel response : responses) {
                        Object val = response.getAnswers().get(key);
                        if (val != null) {
                            boolean boolVal = Boolean.parseBoolean(val.toString());
                            if (boolVal) yesCount++;
                            else noCount++;
                        }
                    }

                    Map<String, Object> boolResult = new HashMap<>();
                    boolResult.put("Yes", yesCount);
                    boolResult.put("No", noCount);

                    results.add(new SurveyResultDTO(title, "boolean", boolResult));
                }
            }
        }

        return results;
    }

    public List<SurveyResultDTO> getSurveyResultsByUserId(String quizSurveyId, String userId) {
        QuizSurveyModel survey = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));

        SurveyDefinition definition = survey.getDefinitionJson();

        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);
        if (responses.isEmpty()) {
            throw new IllegalArgumentException("No responses found for this survey.");
        }

        int totalResponses = responses.size();

        Map<String, Map<String, Integer>> counts = new HashMap<>();
        Map<String, List<Integer>> ratingResponses = new HashMap<>();

        // Get user's response
        List<ResponseModel> userResponses = responseRepo.findByQuizSurveyIdAndUserId(quizSurveyId, userId);
        if (responses.isEmpty()) {
            throw new IllegalArgumentException("Response not found for this user and quiz.");
        }
        ResponseModel userResponse = responses.get(0);

        Map<String, Object> userAnswers = userResponse.getAnswers();

        // Build aggregates
        for (ResponseModel response : responses) {
            Map<String, Object> answers = response.getAnswers();
            for (SurveyDefinition.Page page : definition.getPages()) {
                for (SurveyDefinition.Element element : page.getElements()) {
                    String key = element.getName();
                    String type = element.getType().toLowerCase();
                    if (!answers.containsKey(key)) continue;

                    Object value = answers.get(key);

                    if ("rating".equals(type)) {
                        ratingResponses.putIfAbsent(key, new ArrayList<>());
                        try {
                            int rating = (int) Double.parseDouble(value.toString());
                            ratingResponses.get(key).add(rating);
                        } catch (Exception ignored) {
                        }
                    } else if (element.getChoices() != null) {
                        counts.putIfAbsent(key, new HashMap<>());
                        for (String choice : element.getChoices()) {
                            counts.get(key).putIfAbsent(choice, 0);
                        }

                        if (value instanceof List<?> list) {
                            for (Object val : list) {
                                String choice = val.toString();
                                counts.get(key).computeIfPresent(choice, (k, v) -> v + 1);
                            }
                        } else {
                            String choice = value.toString();
                            counts.get(key).computeIfPresent(choice, (k, v) -> v + 1);
                        }
                    }
                }
            }
        }

        // Final result list
        List<SurveyResultDTO> results = new ArrayList<>();

        // Build final per-user response with aggregate
        for (SurveyDefinition.Page page : definition.getPages()) {
            for (SurveyDefinition.Element element : page.getElements()) {
                String key = element.getName();
                String title = element.getTitle();
                String type = element.getType().toLowerCase();

                Object userValue = userAnswers.get(key);

                if ("rating".equals(type)) {
                    Map<String, Object> ratingResult = new HashMap<>();
                    if (userValue != null) {
                        try {
                            ratingResult.put("value", Integer.parseInt(userValue.toString()));
                        } catch (Exception ignored) {
                        }
                    }
                    results.add(new SurveyResultDTO(title, type, ratingResult));

                } else if (element.getChoices() != null) {
                    String choiceType = switch (type) {
                        case "dropdown", "radiogroup", "checkbox" -> type;
                        default -> "choice";
                    };

                    Map<String, Integer> countMap = counts.getOrDefault(key, new HashMap<>());

                    Set<String> userSelectedChoices = new HashSet<>();
                    if (userValue instanceof List<?> list) {
                        list.forEach(val -> userSelectedChoices.add(val.toString()));
                    } else if (userValue != null) {
                        userSelectedChoices.add(userValue.toString());
                    }

                    Map<String, Object> detailedMap = new LinkedHashMap<>();
                    for (String choice : element.getChoices()) {
                        int count = countMap.getOrDefault(choice, 0);
                        int percentage = (int) Math.round((count * 100.0) / totalResponses);
                        Map<String, Object> choiceMap = new HashMap<>();
                        choiceMap.put("percentage", percentage);
                        choiceMap.put("correct", userSelectedChoices.contains(choice));
                        detailedMap.put(choice, choiceMap);
                    }

                    results.add(new SurveyResultDTO(title, choiceType, detailedMap));

                } else if ("boolean".equals(type)) {
                    Map<String, Object> booleanMap = new HashMap<>();
                    booleanMap.put("value", userValue != null ? Boolean.parseBoolean(userValue.toString()) : null);
                    results.add(new SurveyResultDTO(title, "boolean", booleanMap));

                } else if ("text".equals(type) || "comment".equals(type)) {
                    Map<String, Object> textMap = new HashMap<>();
                    textMap.put("value", userValue != null ? userValue.toString() : null);
                    results.add(new SurveyResultDTO(title, type, textMap));
                }
            }
        }

        return results;
    }
}
