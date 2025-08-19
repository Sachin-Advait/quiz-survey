package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.controller.QuizSurveySocketController;
import com.gissoftware.quiz_survey.dto.*;
import com.gissoftware.quiz_survey.mapper.QuizSurveyMapper;
import com.gissoftware.quiz_survey.mapper.SurveyResponseStatsMapper;
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
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class QuizSurveyService {

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

    // Get Survey Satisfactions Questions Rating
    public SatisfactionInsightResponse getSatisfactionInsights(String surveyId) {
        QuizSurveyModel survey = quizSurveyRepo.findById(surveyId)
                .orElseThrow(() -> new RuntimeException("Survey not found"));

        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(surveyId);

        // Extract rating-type questions from survey definition
        List<SurveyDefinition.Element> ratingQuestions = survey.getDefinitionJson().getPages().stream()
                .flatMap(page -> page.getElements().stream())
                .filter(element -> "rating".equalsIgnoreCase(element.getType()))
                .toList();

        Map<String, List<Integer>> questionRatingsMap = new HashMap<>();

        for (SurveyDefinition.Element q : ratingQuestions) {
            questionRatingsMap.put(q.getTitle(), new ArrayList<>());
        }

        for (ResponseModel response : responses) {
            Map<String, Object> answers = response.getAnswers();

            for (SurveyDefinition.Element q : ratingQuestions) {
                Object val = answers.get(q.getName());
                if (val instanceof Number) {
                    questionRatingsMap.get(q.getTitle()).add(((Number) val).intValue());
                }
            }
        }

        // Build distribution
        List<SatisfactionInsightResponse.QuestionDistribution> distributions = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> entry : questionRatingsMap.entrySet()) {
            Map<Integer, Integer> distribution = new HashMap<>();
            for (int i = 1; i <= 5; i++) distribution.put(i, 0);

            for (Integer rating : entry.getValue()) {
                distribution.put(rating, distribution.getOrDefault(rating, 0) + 1);
            }

            distributions.add(SatisfactionInsightResponse.QuestionDistribution.builder()
                    .question(entry.getKey())
                    .distribution(distribution)
                    .build());
        }

        double avg = questionRatingsMap.values().stream()
                .flatMap(Collection::stream)
                .mapToInt(Integer::intValue)
                .average().orElse(0.0);

        return SatisfactionInsightResponse.builder()
                .title("Satisfaction Insights")
                .averageSatisfactionBySurveyType(Map.of(survey.getTitle(), avg))
                .scoreDistributionPerQuestion(distributions)
                .build();
    }

    // Get Survey Most and Least Active Regions & Outlet
    public SurveyActivityStatsDTO getSurveyActivityStats(String surveyId) {
        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(surveyId);

        // Map of userId -> UserModel for efficient lookup
        Map<String, UserModel> userMap = userRepository
                .findAllById(responses.stream().map(ResponseModel::getUserId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(UserModel::getId, Function.identity()));

        // Count responses per region
        Map<String, Long> regionCounts = responses.stream()
                .map(r -> userMap.get(r.getUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        u -> Optional.ofNullable(u.getRegion()).orElse("Unknown"),
                        Collectors.counting()
                ));

        // Count responses per outlet
        Map<String, Long> outletCounts = responses.stream()
                .map(r -> userMap.get(r.getUserId()))
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        u -> {
                            if (u.getOutlet() != null) {
                                return u.getOutlet();
                            } else {
                                return "Unknown";
                            }
                        },
                        Collectors.counting()
                ));


        List<SurveyActivityStatsDTO.RegionActivityDTO> sortedRegions = regionCounts.entrySet().stream()
                .map(e -> new SurveyActivityStatsDTO.RegionActivityDTO(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparingInt(SurveyActivityStatsDTO.RegionActivityDTO::getResponses).reversed())
                .toList();

        List<SurveyActivityStatsDTO.OutletActivityDTO> sortedOutlets = outletCounts.entrySet().stream()
                .map(e -> new SurveyActivityStatsDTO.OutletActivityDTO(e.getKey(), e.getValue().intValue()))
                .sorted(Comparator.comparingInt(SurveyActivityStatsDTO.OutletActivityDTO::getResponses).reversed())
                .toList();

        return SurveyActivityStatsDTO.builder()
                .mostActiveRegions(sortedRegions.stream().limit(3).toList())
                .leastActiveRegions(sortedRegions.stream().sorted(Comparator.comparingInt(SurveyActivityStatsDTO.RegionActivityDTO::getResponses)).limit(3).toList())
                .mostActiveOutlets(sortedOutlets.stream().limit(3).toList())
                .leastActiveOutlets(sortedOutlets.stream().sorted(Comparator.comparingInt(SurveyActivityStatsDTO.OutletActivityDTO::getResponses)).limit(3).toList())
                .build();
    }

    // Get Survey Overall Completion Status and by Region, outlet, and Role
    public SurveyResponseStatsDTO getSurveyInsightStats(String surveyId) {
        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(surveyId);
        QuizSurveyModel survey = quizSurveyRepo.findById(surveyId)
                .orElseThrow(() -> new RuntimeException("Survey not found"));

        List<UserModel> invitedUsers = userRepository.findAllById(survey.getTargetedUsers());

        int totalInvited = invitedUsers.size();
        int totalResponded = (int) responses.stream()
                .map(ResponseModel::getUserId)
                .distinct()
                .count();
        double overallRate = calculateRate(totalResponded, totalInvited);

        List<SurveyResponseStatsDTO.BreakdownByRegionDTO> regionBreakdowns = buildHierarchicalBreakdown(invitedUsers, responses);

        return SurveyResponseStatsDTO.builder()
                .title(survey.getTitle())
                .overall(surveyResponseStatsMapper.toOverallStats(totalInvited, totalResponded, overallRate))
                .byRegion(regionBreakdowns)
                .build();
    }

    // ========== GENERIC BREAKDOWN ==========
    private List<SurveyResponseStatsDTO.BreakdownByRegionDTO> buildHierarchicalBreakdown(
            List<UserModel> invitedUsers, List<ResponseModel> responses) {
        Map<String, Map<String, Map<String, List<UserModel>>>> groupedInvited = invitedUsers.stream()
                .collect(Collectors.groupingBy(
                        u -> safeString(u.getRegion()),
                        Collectors.groupingBy(
                                u -> safeString(u.getOutlet()),
                                Collectors.groupingBy(
                                        u -> safeString(u.getPosition())
                                )
                        )
                ));

        return groupedInvited.entrySet().stream()
                .map(regionEntry -> {
                    String region = regionEntry.getKey();
                    Map<String, Map<String, List<UserModel>>> outletMap = regionEntry.getValue();
                    System.out.println(outletMap);

                    List<SurveyResponseStatsDTO.BreakdownByOutletDTO> outletDTOs = outletMap.entrySet().stream()
                            .map(outletEntry -> {
                                String outlet = outletEntry.getKey();
                                Map<String, List<UserModel>> roleMap = outletEntry.getValue();

                                List<SurveyResponseStatsDTO.BreakdownByRoleDTO> roleDTOs = roleMap.entrySet().stream()
                                        .map(roleEntry -> {
                                            String role = roleEntry.getKey();
                                            List<UserModel> roleUsers = roleEntry.getValue();
                                            int invited = roleUsers.size();
                                            int responded = (int) roleUsers.stream()
                                                    .map(UserModel::getId)
                                                    .filter(id -> responses.stream().anyMatch(r -> r.getUserId().equals(id)))
                                                    .count();
                                            return surveyResponseStatsMapper.toRoleBreakdown(role, responded, invited, calculateRate(responded, invited));
                                        }).collect(Collectors.toList());

                                int outletInvited = roleMap.values().stream().mapToInt(List::size).sum();
                                int outletResponded = roleDTOs.stream().mapToInt(SurveyResponseStatsDTO.BreakdownByRoleDTO::getResponded).sum();
                                return surveyResponseStatsMapper.toOutletBreakdown(outlet, outletResponded, outletInvited, calculateRate(outletResponded, outletInvited), roleDTOs);
                            }).collect(Collectors.toList());

                    int regionInvited = outletDTOs.stream().mapToInt(SurveyResponseStatsDTO.BreakdownByOutletDTO::getInvited).sum();
                    int regionResponded = outletDTOs.stream().mapToInt(SurveyResponseStatsDTO.BreakdownByOutletDTO::getResponded).sum();
                    return surveyResponseStatsMapper.toRegionBreakdown(region, regionResponded, regionInvited, calculateRate(regionResponded, regionInvited), outletDTOs);
                }).collect(Collectors.toList());
    }

    // ========== HELPERS ==========
    private <T> String safeString(String value) {
        return value != null && !value.trim().isEmpty() ? value : "Unknown";
    }

    private double calculateRate(int responded, int invited) {
        return invited == 0 ? 0.0 : Math.round((responded * 10000.0 / invited)) / 100.0;
    }
}
