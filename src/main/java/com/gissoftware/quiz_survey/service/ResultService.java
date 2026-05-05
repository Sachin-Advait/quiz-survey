package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.QuizResultAdminDTO;
import com.gissoftware.quiz_survey.dto.QuizResultDTO;
import com.gissoftware.quiz_survey.dto.SurveyResultDTO;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.SurveyDefinition;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ResultService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final ResponseRepo responseRepo;
    private final UserRepository userRepository;

    // -------------------- QUIZ RESULTS --------------------

    public List<QuizResultAdminDTO> getQuizResultsAdmin(String quizSurveyId) {
        QuizSurveyModel quizSurvey = getQuizSurveyOrThrow(quizSurveyId);
        SurveyDefinition definition = quizSurvey.getDefinitionJson();
        // FIX: answerKey can be null
        Map<String, Object> answerKey = Optional.ofNullable(quizSurvey.getAnswerKey())
                .orElse(Collections.emptyMap());

        List<ResponseModel> responses = Optional.ofNullable(responseRepo.findByQuizSurveyId(quizSurveyId))
                .orElse(Collections.emptyList());

        return responses.stream()
                .collect(Collectors.groupingBy(r -> Optional.ofNullable(r.getUserId()).orElse("unknown")))
                .entrySet()
                .stream()
                .map(entry -> {
                    String userId = entry.getKey();
                    List<ResponseModel> userResponses = entry.getValue();

                    List<QuizResultDTO> attemptsDTO = userResponses.stream()
                            .map(resp -> mapQuizResponseToDTO(resp, definition, answerKey))
                            .toList();

                    // FIX: safely get username from first non-null entry
                    String username = userResponses.stream()
                            .map(ResponseModel::getUsername)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse("Unknown");

                    return QuizResultAdminDTO.builder()
                            .id(userId)
                            .username(username)
                            .attempts(attemptsDTO)
                            .build();
                })
                .toList();
    }

    public QuizResultDTO getQuizResultByUserId(String quizSurveyId, String userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid userId"));

        QuizSurveyModel quizSurvey = getQuizSurveyOrThrow(quizSurveyId);

        if (Boolean.FALSE.equals(quizSurvey.getIsAnnounced())) {
            throw new IllegalStateException("Results are not announced yet.");
        }

        // FIX: answerKey can be null
        Map<String, Object> answerKey = Optional.ofNullable(quizSurvey.getAnswerKey())
                .orElse(Collections.emptyMap());

        List<ResponseModel> userResponses = Optional.ofNullable(
                        responseRepo.findByQuizSurveyIdAndUserId(quizSurveyId, userId))
                .orElse(Collections.emptyList());

        ResponseModel highestScoreResp = userResponses.stream()
                .max(Comparator.comparing(
                        ResponseModel::getScore,
                        Comparator.nullsLast(Integer::compareTo)))
                .orElseThrow(() -> new IllegalArgumentException("No responses found."));

        return mapQuizResponseToDTO(highestScoreResp, quizSurvey.getDefinitionJson(), answerKey);
    }

    // -------------------- SURVEY RESULTS --------------------

    public List<SurveyResultDTO> getSurveyResultsAdmin(String quizSurveyId, String userId) {
        QuizSurveyModel survey = getQuizSurveyOrThrow(quizSurveyId);
        SurveyDefinition definition = survey.getDefinitionJson();

        // FIX: definition or pages can be null
        if (definition == null || definition.getPages() == null) {
            return Collections.emptyList();
        }

        List<ResponseModel> responses = Optional.ofNullable(responseRepo.findByQuizSurveyId(quizSurveyId))
                .orElse(Collections.emptyList());
        if (responses.isEmpty()) throw new IllegalArgumentException("No responses found for this survey.");

        Map<String, Map<String, Integer>> counts = new HashMap<>();
        Map<String, List<Integer>> ratings = new HashMap<>();

        // Preprocess questions
        for (SurveyDefinition.Page page : definition.getPages()) {
            if (page == null || page.getElements() == null) continue;
            for (SurveyDefinition.Element el : page.getElements()) {
                if (el == null || el.getName() == null || el.getType() == null) continue;
                String key = el.getName();
                switch (el.getType().toLowerCase()) {
                    case "rating" -> ratings.put(key, new ArrayList<>());
                    case "radiogroup", "checkbox", "dropdown" -> {
                        // FIX: choices can be null
                        List<String> choices = Optional.ofNullable(el.getChoices())
                                .orElse(Collections.emptyList());
                        Map<String, Integer> map = choices.stream()
                                .collect(Collectors.toMap(c -> c, c -> 0));
                        counts.put(key, map);
                    }
                }
            }
        }

        for (ResponseModel resp : responses) {
            // FIX: answers map can be null
            Map<String, Object> answers = Optional.ofNullable(resp.getAnswers())
                    .orElse(Collections.emptyMap());
            answers.forEach((key, value) -> {
                if (counts.containsKey(key)) incrementCounts(counts.get(key), value);
                else if (ratings.containsKey(key)) addRating(ratings.get(key), value);
            });
        }

        return definition.getPages().stream()
                .filter(Objects::nonNull)
                .filter(page -> page.getElements() != null)
                .flatMap(page -> page.getElements().stream())
                .filter(Objects::nonNull)
                .map(el -> buildAdminSurveyResultDTO(el, counts, ratings, responses.size(), survey.getId()))
                .toList();
    }

    public List<SurveyResultDTO> getSurveyResultsByUserId(String quizSurveyId, String userId) {
        QuizSurveyModel survey = getQuizSurveyOrThrow(quizSurveyId);
        SurveyDefinition definition = survey.getDefinitionJson();

        // FIX: definition or pages can be null
        if (definition == null || definition.getPages() == null) {
            return Collections.emptyList();
        }

        List<ResponseModel> responses = Optional.ofNullable(responseRepo.findByQuizSurveyId(quizSurveyId))
                .orElse(Collections.emptyList());
        if (responses.isEmpty()) throw new IllegalArgumentException("No responses found.");

        ResponseModel userResponse = Optional.ofNullable(
                        responseRepo.findByQuizSurveyIdAndUserId(quizSurveyId, userId))
                .orElse(Collections.emptyList())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Response not found for this user and quiz."));

        // FIX: userAnswers can be null
        Map<String, Object> userAnswers = Optional.ofNullable(userResponse.getAnswers())
                .orElse(Collections.emptyMap());

        Map<String, Map<String, Integer>> counts = new HashMap<>();
        Map<String, List<Integer>> ratings = new HashMap<>();

        for (ResponseModel resp : responses) {
            // FIX: answers can be null
            Map<String, Object> answers = Optional.ofNullable(resp.getAnswers())
                    .orElse(Collections.emptyMap());
            answers.forEach((key, value) -> {
                SurveyDefinition.Element el = definition.getPages().stream()
                        .filter(Objects::nonNull)
                        .filter(p -> p.getElements() != null)
                        .flatMap(p -> p.getElements().stream())
                        .filter(Objects::nonNull)
                        .filter(e -> key.equals(e.getName()))
                        .findFirst()
                        .orElse(null);
                if (el == null || el.getType() == null) return;

                if (el.getType().equalsIgnoreCase("rating")) {
                    addRating(ratings.computeIfAbsent(key, k -> new ArrayList<>()), value);
                } else {
                    if (el.getChoices() != null) {
                        counts.putIfAbsent(key, el.getChoices().stream()
                                .collect(Collectors.toMap(c -> c, c -> 0)));
                        incrementCounts(counts.get(key), value);
                    }
                }
            });
        }

        return definition.getPages().stream()
                .filter(Objects::nonNull)
                .filter(page -> page.getElements() != null)
                .flatMap(page -> page.getElements().stream())
                .filter(Objects::nonNull)
                .map(el -> buildUserSurveyResultDTO(el, counts, userAnswers, responses.size(), quizSurveyId))
                .toList();
    }

    // -------------------- HELPERS --------------------

    private SurveyResultDTO buildAdminSurveyResultDTO(SurveyDefinition.Element el,
                                                      Map<String, Map<String, Integer>> counts,
                                                      Map<String, List<Integer>> ratings,
                                                      int totalResponses,
                                                      String quizId) {
        // FIX: name / type / title can be null
        String key = Optional.ofNullable(el.getName()).orElse("");
        String type = Optional.ofNullable(el.getType()).orElse("").toLowerCase();
        String title = el.getTitle();
        String arabicTitle = el.getArabicTitle();

        if (counts.containsKey(key)) {
            Map<String, Object> result = new HashMap<>();
            counts.get(key).forEach((choice, count) -> {
                int percentage = totalResponses > 0
                        ? (int) Math.round(count * 100.0 / totalResponses) : 0;
                result.put(choice, Map.of("percentage", percentage));
            });
            return new SurveyResultDTO(title, arabicTitle, type, result);

        } else if (ratings.containsKey(key)) {
            List<Integer> ratingList = ratings.get(key);
            double avg = ratingList.stream().mapToInt(i -> i).average().orElse(0.0);
            Map<String, Object> result = Map.of(
                    "averageRating", avg,
                    "responseCount", ratingList.size());
            return new SurveyResultDTO(title, arabicTitle, "rating", result);

        } else if ("boolean".equals(type)) {
            int yes = 0, no = 0;
            List<ResponseModel> allResponses = Optional.ofNullable(responseRepo.findByQuizSurveyId(quizId))
                    .orElse(Collections.emptyList());
            for (ResponseModel resp : allResponses) {
                // FIX: answers can be null
                Map<String, Object> answers = Optional.ofNullable(resp.getAnswers())
                        .orElse(Collections.emptyMap());
                Object val = answers.get(key);
                if (val != null && Boolean.parseBoolean(val.toString())) yes++;
                else no++;
            }
            int total = yes + no;
            int yesPercentage = total > 0 ? (int) Math.round((yes * 100.0) / total) : 0;
            int noPercentage  = total > 0 ? (int) Math.round((no  * 100.0) / total) : 0;
            Map<String, Object> boolResult = Map.of(
                    "Yes", Map.of("percentage", yesPercentage),
                    "No",  Map.of("percentage", noPercentage));
            return new SurveyResultDTO(title, arabicTitle, type, boolResult);
        }

        return new SurveyResultDTO(title, arabicTitle, type, Collections.emptyMap());
    }

    private SurveyResultDTO buildUserSurveyResultDTO(SurveyDefinition.Element el,
                                                     Map<String, Map<String, Integer>> counts,
                                                     Map<String, Object> userAnswers,
                                                     int totalResponses,
                                                     String surveyId) {
        // FIX: name / type can be null
        String key   = Optional.ofNullable(el.getName()).orElse("");
        String type  = Optional.ofNullable(el.getType()).orElse("").toLowerCase();
        String title = el.getTitle();
        String arabicTitle = el.getArabicTitle();
        Object userValue = userAnswers.get(key);

        switch (type) {
            case "rating" -> {
                // FIX: userValue can be null
                return new SurveyResultDTO(title, arabicTitle, type,
                        Map.of("value", userValue != null ? userValue : ""));
            }
            case "radiogroup", "checkbox", "dropdown" -> {
                Map<String, Integer> countMap = counts.getOrDefault(key, new HashMap<>());
                Map<String, Object> detailedMap = new LinkedHashMap<>();

                Set<String> userSelected = new HashSet<>();
                if (userValue instanceof List<?> list) {
                    list.stream().filter(Objects::nonNull).forEach(v -> userSelected.add(v.toString()));
                } else if (userValue != null) {
                    userSelected.add(userValue.toString());
                }

                List<String> choices = Optional.ofNullable(el.getChoices())
                        .orElse(Collections.emptyList());
                for (String choice : choices) {
                    int count = countMap.getOrDefault(choice, 0);
                    int percentage = totalResponses > 0
                            ? (int) Math.round(count * 100.0 / totalResponses) : 0;
                    detailedMap.put(choice, Map.of(
                            "percentage", percentage,
                            "isSelect",   userSelected.contains(choice)));
                }
                return new SurveyResultDTO(title, arabicTitle, type, detailedMap);
            }
            case "boolean" -> {
                int yes = 0, no = 0;
                List<ResponseModel> allResponses = Optional.ofNullable(responseRepo.findByQuizSurveyId(surveyId))
                        .orElse(Collections.emptyList());
                for (ResponseModel resp : allResponses) {
                    // FIX: answers can be null
                    Map<String, Object> answers = Optional.ofNullable(resp.getAnswers())
                            .orElse(Collections.emptyMap());
                    Object val = answers.get(key);
                    if (val != null && Boolean.parseBoolean(val.toString())) yes++;
                    else no++;
                }
                int total = yes + no;
                int yesPercentage = total > 0 ? (int) Math.round((yes * 100.0) / total) : 0;
                int noPercentage  = total > 0 ? (int) Math.round((no  * 100.0) / total) : 0;

                // FIX: safely determine user's boolean selection
                boolean userChoseYes = userValue != null && Boolean.parseBoolean(userValue.toString());
                boolean userChoseNo  = userValue != null && !Boolean.parseBoolean(userValue.toString());

                Map<String, Object> boolResult = Map.of(
                        "Yes", Map.of("percentage", yesPercentage, "isSelect", userChoseYes),
                        "No",  Map.of("percentage", noPercentage,  "isSelect", userChoseNo));
                return new SurveyResultDTO(title, arabicTitle, type, boolResult);
            }
        }

        // Fallback for text / comment / other types
        return new SurveyResultDTO(
                title,
                arabicTitle,
                type,
                Map.of("value", userValue != null ? userValue.toString() : ""));
    }

    private QuizSurveyModel getQuizSurveyOrThrow(String id) {
        return quizSurveyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz survey not found"));
    }

    private QuizResultDTO mapQuizResponseToDTO(ResponseModel resp,
                                               SurveyDefinition definition,
                                               Map<String, Object> answerKey) {
        // FIX: answers / definition / pages can be null
        Map<String, Object> selectedAnswers = Optional.ofNullable(resp.getAnswers())
                .orElse(Collections.emptyMap());
        Map<String, QuizResultDTO.QuestionAnswerDTO> formattedAnswers = new LinkedHashMap<>();

        if (definition == null || definition.getPages() == null) {
            return QuizResultDTO.builder()
                    .id(resp.getId())
                    .username(resp.getUsername())
                    .score(resp.getScore())
                    .maxScore(resp.getMaxScore())
                    .submittedAt(resp.getSubmittedAt())
                    .answers(formattedAnswers)
                    .finishTime(resp.getFinishTime())
                    .build();
        }

        for (SurveyDefinition.Page page : definition.getPages()) {
            if (page == null || page.getElements() == null) continue;
            for (SurveyDefinition.Element q : page.getElements()) {
                if (q == null) continue;

                String questionText = Optional.ofNullable(q.getTitle()).orElse("");
                String questionId   = Optional.ofNullable(q.getName()).orElse("");
                String questionType = Optional.ofNullable(q.getType()).orElse("");

                List<QuizResultDTO.OptionDTO> options = Optional.ofNullable(q.getChoices())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(choice -> {
                            boolean correct = false;
                            // FIX: answerKey or its value can be null
                            Object ans = answerKey.get(questionId);
                            if (ans instanceof String s) {
                                correct = choice.equals(s);
                            } else if (ans instanceof List<?> list) {
                                correct = list.contains(choice);
                            }
                            return QuizResultDTO.OptionDTO.builder()
                                    .text(choice)
                                    .isCorrect(correct)
                                    .build();
                        })
                        .toList();

                Object selected    = selectedAnswers.get(questionId);
                Object selectedOpt = formatSelectedOptions(selected);

                // FIX: correctAnswer can be null; check type safely
                String correctAnswer = null;
                if ("text".equalsIgnoreCase(questionType) && q.getCorrectAnswer() != null) {
                    correctAnswer = q.getCorrectAnswer().toString();
                }

                formattedAnswers.put(questionText, QuizResultDTO.QuestionAnswerDTO.builder()
                        .choices(options)
                        .type(questionType)
                        .correctAnswer(correctAnswer)
                        .selectedOptions(selectedOpt)
                        .arabicTitle(q.getArabicTitle())
                        .mark(q.getMarks())
                        .build());
            }
        }

        return QuizResultDTO.builder()
                .id(resp.getId())
                .username(resp.getUsername())
                .score(resp.getScore())
                .maxScore(resp.getMaxScore())
                .submittedAt(resp.getSubmittedAt())
                .answers(formattedAnswers)
                .finishTime(resp.getFinishTime())
                .build();
    }

    private Object formatSelectedOptions(Object selected) {
        if (selected instanceof List<?> list) {
            if (list.size() == 1) return list.get(0) != null ? list.get(0).toString() : null;
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toList();
        }
        return selected != null ? selected.toString() : null;
    }

    private void incrementCounts(Map<String, Integer> counts, Object value) {
        if (value instanceof List<?> list) {
            list.stream()
                    .filter(Objects::nonNull)
                    .forEach(v -> counts.computeIfPresent(v.toString(), (k, v1) -> v1 + 1));
        } else if (value != null) {
            counts.computeIfPresent(value.toString(), (k, v) -> v + 1);
        }
    }

    private void addRating(List<Integer> ratings, Object value) {
        if (value == null) return;
        try {
            ratings.add((int) Double.parseDouble(value.toString()));
        } catch (NumberFormatException ignored) {
        }
    }
}