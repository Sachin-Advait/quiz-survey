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
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
        Map<String, Object> answerKey = quizSurvey.getAnswerKey();

        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);

        return responses.stream()
                .collect(Collectors.groupingBy(ResponseModel::getUserId))
                .entrySet()
                .stream()
                .map(entry -> {
                    String userId = entry.getKey();
                    List<QuizResultDTO> attemptsDTO = entry.getValue().stream()
                            .map(resp -> mapQuizResponseToDTO(resp, definition, answerKey))
                            .toList();
                    return QuizResultAdminDTO.builder()
                            .id(userId)
                            .username(entry.getValue().get(0).getUsername())
                            .attempts(attemptsDTO)
                            .build();
                })
                .toList();
    }

    public QuizResultDTO getQuizResultByUserId(String quizSurveyId, String userId) {

        userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Invalid userId"));
        QuizSurveyModel quizSurvey = getQuizSurveyOrThrow(quizSurveyId);

        if (!quizSurvey.getIsAnnounced()) {
            throw new IllegalStateException("Results are not announced yet.");
        }

        Map<String, Object> answerKey = quizSurvey.getAnswerKey();

        ResponseModel highestScoreResp = responseRepo
                .findByQuizSurveyIdAndUserId(quizSurveyId, userId)
                .stream()
                .max(Comparator.comparing(ResponseModel::getScore, Comparator.nullsLast(Integer::compareTo)))
                .orElseThrow(() -> new IllegalArgumentException("No responses found."));

        return mapQuizResponseToDTO(highestScoreResp, quizSurvey.getDefinitionJson(), answerKey);
    }

    // -------------------- SURVEY RESULTS --------------------
    public List<SurveyResultDTO> getSurveyResultsAdmin(String quizSurveyId, String userId) {
        QuizSurveyModel survey = getQuizSurveyOrThrow(quizSurveyId);
        SurveyDefinition definition = survey.getDefinitionJson();

        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);
        if (responses.isEmpty()) throw new IllegalArgumentException("No responses found for this survey.");

        Map<String, Map<String, Integer>> counts = new HashMap<>();
        Map<String, List<Integer>> ratings = new HashMap<>();

        // Preprocess responses
        for (SurveyDefinition.Page page : definition.getPages()) {
            for (SurveyDefinition.Element el : page.getElements()) {
                String key = el.getName();
                switch (el.getType().toLowerCase()) {
                    case "rating" -> ratings.put(key, new ArrayList<>());
                    case "radiogroup", "checkbox", "dropdown" -> {
                        Map<String, Integer> map = el.getChoices().stream()
                                .collect(Collectors.toMap(c -> c, c -> 0));
                        counts.put(key, map);
                    }
                }
            }
        }

        for (ResponseModel resp : responses) {
            Map<String, Object> answers = resp.getAnswers();
            answers.forEach((key, value) -> {
                if (counts.containsKey(key)) incrementCounts(counts.get(key), value);
                else if (ratings.containsKey(key)) addRating(ratings.get(key), value);
            });
        }

        return definition.getPages().stream()
                .flatMap(page -> page.getElements().stream())
                .map(el -> buildAdminSurveyResultDTO(el, counts, ratings, responses.size(), survey.getId()))
                .toList();
    }

    public List<SurveyResultDTO> getSurveyResultsByUserId(String quizSurveyId, String userId) {
        QuizSurveyModel survey = getQuizSurveyOrThrow(quizSurveyId);
        SurveyDefinition definition = survey.getDefinitionJson();

        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);
        if (responses.isEmpty()) throw new IllegalArgumentException("No responses found.");

        ResponseModel userResponse = responseRepo.findByQuizSurveyIdAndUserId(quizSurveyId, userId)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Response not found for this user and quiz."));

        Map<String, Object> userAnswers = userResponse.getAnswers();
        Map<String, Map<String, Integer>> counts = new HashMap<>();
        Map<String, List<Integer>> ratings = new HashMap<>();

        for (ResponseModel resp : responses) {
            resp.getAnswers().forEach((key, value) -> {
                SurveyDefinition.Element el = definition.getPages().stream()
                        .flatMap(p -> p.getElements().stream())
                        .filter(e -> e.getName().equals(key))
                        .findFirst().orElse(null);
                if (el == null) return;
                if (el.getType().equalsIgnoreCase("rating")) {
                    addRating(ratings.computeIfAbsent(key, k -> new ArrayList<>()), value);
                } else {
                    if (el.getChoices() != null) {
                        counts.putIfAbsent(key, el.getChoices().stream().collect(Collectors.toMap(c -> c, c -> 0)));
                        incrementCounts(counts.get(key), value);
                    }
                }
            });
        }

        return definition.getPages().stream()
                .flatMap(page -> page.getElements().stream())
                .map(el -> buildUserSurveyResultDTO(el, counts, userAnswers, responses.size(), quizSurveyId))
                .toList();
    }

    // -------------------- HELPERS --------------------
    private SurveyResultDTO buildAdminSurveyResultDTO(SurveyDefinition.Element el,
                                                      Map<String, Map<String, Integer>> counts,
                                                      Map<String, List<Integer>> ratings,
                                                      int totalResponses, String quizId) {
        String key = el.getName();
        String type = el.getType().toLowerCase();
        String title = el.getTitle();

        if (counts.containsKey(key)) {
            Map<String, Object> result = new HashMap<>();
            counts.get(key).forEach((choice, count) -> {
                Map<String, Object> choiceMap = new HashMap<>();
                choiceMap.put("percentage", (int) Math.round(count * 100.0 / totalResponses));
                result.put(choice, choiceMap);
            });
            return new SurveyResultDTO(title, el.getArabicTitle(), type, result);
        } else if (ratings.containsKey(key)) {
            List<Integer> ratingList = ratings.get(key);
            double avg = ratingList.stream().mapToInt(i -> i).average().orElse(0.0);
            Map<String, Object> result = Map.of("averageRating", avg, "responseCount", ratingList.size());
            return new SurveyResultDTO(title, el.getArabicTitle(), "rating", result);
        } else if ("boolean".equals(type)) {
            int yes = 0, no = 0;
            for (ResponseModel resp : responseRepo.findByQuizSurveyId(quizId)) {
                Object val = resp.getAnswers().get(key);
                if (val != null && Boolean.parseBoolean(String.valueOf(val))) {
                    yes++;
                } else {
                    no++;
                }
            }

            int total = yes + no;
            int yesPercentage = total > 0 ? (int) Math.round((yes * 100.0) / total) : 0;
            int noPercentage = total > 0 ? (int) Math.round((no * 100.0) / total) : 0;

            Map<String, Object> boolResult = Map.of(
                    "Yes", Map.of("percentage", yesPercentage),
                    "No", Map.of("percentage", noPercentage)
            );

            return new SurveyResultDTO(title, el.getArabicTitle(), type, boolResult);

        } else return new SurveyResultDTO(title, el.getArabicTitle(), type, Collections.emptyMap());
    }

    private SurveyResultDTO buildUserSurveyResultDTO(SurveyDefinition.Element el,
                                                     Map<String, Map<String, Integer>> counts,
                                                     Map<String, Object> userAnswers,
                                                     int totalResponses, String surveyId) {
        String key = el.getName();
        String type = el.getType().toLowerCase();
        String title = el.getTitle();
        Object userValue = userAnswers.get(key);

        switch (type) {
            case "rating" -> {
                return new SurveyResultDTO(title, el.getArabicTitle(), type, Map.of("value", userValue));
            }
            case "radiogroup", "checkbox", "dropdown" -> {
                System.out.println(el.getChoices());
                Map<String, Integer> countMap = counts.getOrDefault(key, new HashMap<>());
                Map<String, Object> detailedMap = new LinkedHashMap<>();
                Set<String> userSelected = new HashSet<>();
                if (userValue instanceof List<?> list) list.forEach(v -> userSelected.add(v.toString()));
                else if (userValue != null) userSelected.add(userValue.toString());


                for (String choice : el.getChoices()) {
                    int count = countMap.getOrDefault(choice, 0);
                    int percentage = (int) Math.round(count * 100.0 / totalResponses);
                    detailedMap.put(choice, Map.of("percentage", percentage, "isSelect", userSelected.contains(choice)));
                }
                return new SurveyResultDTO(title, el.getArabicTitle(), type, detailedMap);
            }
            case "boolean" -> {
                int yes = 0, no = 0;
                for (ResponseModel resp : responseRepo.findByQuizSurveyId(surveyId)) {
                    Object val = resp.getAnswers().get(key);
                    if (val != null && Boolean.parseBoolean(String.valueOf(val))) {
                        yes++;
                    } else {
                        no++;
                    }
                }

                int total = yes + no;
                int yesPercentage = total > 0 ? (int) Math.round((yes * 100.0) / total) : 0;
                int noPercentage = total > 0 ? (int) Math.round((no * 100.0) / total) : 0;


                Map<String, Object> boolResult = Map.of(
                        "Yes", Map.of("percentage", yesPercentage, "isSelect", Boolean.TRUE.equals(userAnswers.get(key))),
                        "No", Map.of("percentage", noPercentage, "isSelect", Boolean.FALSE.equals(userAnswers.get(key)))
                );

                return new SurveyResultDTO(title, el.getArabicTitle(), type, boolResult);
            }
        }


        return new SurveyResultDTO(title, el.getArabicTitle(), type, Map.of("value", userValue.toString()));
    }

    private QuizSurveyModel getQuizSurveyOrThrow(String id) {
        return quizSurveyRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Quiz survey not found"));
    }

    private QuizResultDTO mapQuizResponseToDTO(ResponseModel resp, SurveyDefinition definition, Map<String, Object> answerKey) {
        Map<String, Object> selectedAnswers = resp.getAnswers();
        Map<String, QuizResultDTO.QuestionAnswerDTO> formattedAnswers = new LinkedHashMap<>();

        for (SurveyDefinition.Page page : definition.getPages()) {
            for (SurveyDefinition.Element q : page.getElements()) {
                String questionText = q.getTitle();
                String questionId = q.getName();

                List<QuizResultDTO.OptionDTO> options = Optional.ofNullable(q.getChoices())
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(choice -> {
                            boolean correct = false;
                            Object ans = answerKey.get(questionId);
                            if (ans instanceof String s) correct = choice.equals(s);
                            else if (ans instanceof List<?> list) correct = list.contains(choice);
                            return QuizResultDTO.OptionDTO.builder().text(choice).isCorrect(correct).build();
                        }).toList();

                Object selected = selectedAnswers.get(questionId);
                Object selectedOpt = formatSelectedOptions(selected);

                formattedAnswers.put(questionText, QuizResultDTO.QuestionAnswerDTO.builder()
                        .choices(options)
                        .type(q.getType())
                        .correctAnswer(Objects.equals(q.getType(), "text") && q.getCorrectAnswer() != null
                                ? q.getCorrectAnswer().toString() : null)
                        .selectedOptions(selectedOpt)
                        .arabicTitle(q.getArabicTitle())
                        .mark(q.getMark())
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
            if (list.size() == 1) return list.get(0).toString();
            else return list.stream().map(String::valueOf).toList();
        } else return selected != null ? selected.toString() : null;
    }

    private void incrementCounts(Map<String, Integer> counts, Object value) {
        if (value instanceof List<?> list) {
            list.forEach(v -> counts.computeIfPresent(v.toString(), (k, v1) -> v1 + 1));
        } else if (value != null) counts.computeIfPresent(value.toString(), (k, v) -> v + 1);
    }

    private void addRating(List<Integer> ratings, Object value) {
        try {
            ratings.add((int) Double.parseDouble(value.toString()));
        } catch (Exception ignored) {
        }
    }


}

