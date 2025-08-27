package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.SatisfactionInsightResponse;
import com.gissoftware.quiz_survey.dto.SurveyActivityStatsDTO;
import com.gissoftware.quiz_survey.dto.SurveyResponseStatsDTO;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class AdminSurveyService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final ResponseRepo responseRepo;
    private final UserRepository userRepository;
    private final SurveyResponseStatsMapper surveyResponseStatsMapper;


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

    public SurveyActivityStatsDTO getSurveyActivityStats(String surveyId) {
        // Get all responses for the survey
        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(surveyId);

        // Get the survey and the invited users
        Optional<QuizSurveyModel> surveyOpt = quizSurveyRepo.findById(surveyId);
        if (surveyOpt.isEmpty()) {
            return SurveyActivityStatsDTO.builder().build();
        }

        List<UserModel> invitedUsers = userRepository.findAllById(
                surveyOpt.get().getTargetedUsers()
        );

        // Map of userId -> number of responses
        Set<String> respondedUserIds = responses.stream()
                .map(ResponseModel::getUserId)
                .collect(Collectors.toSet());

        // Count responses per region
        Map<String, Long> regionResponded = invitedUsers.stream()
                .filter(u -> respondedUserIds.contains(u.getId()))
                .collect(Collectors.groupingBy(
                        u -> Optional.ofNullable(u.getRegion()).orElse("Unknown"),
                        Collectors.counting()
                ));

        // Count non-responded per region
        Map<String, Long> regionNonResponded = invitedUsers.stream()
                .filter(u -> !respondedUserIds.contains(u.getId()))
                .collect(Collectors.groupingBy(
                        u -> Optional.ofNullable(u.getRegion()).orElse("Unknown"),
                        Collectors.counting()
                ));

        // Count responses per outlet
        Map<String, Long> outletResponded = invitedUsers.stream()
                .filter(u -> respondedUserIds.contains(u.getId()))
                .collect(Collectors.groupingBy(
                        u -> Optional.ofNullable(u.getOutlet()).orElse("Unknown"),
                        Collectors.counting()
                ));

        // Count non-responded per outlet
        Map<String, Long> outletNonResponded = invitedUsers.stream()
                .filter(u -> !respondedUserIds.contains(u.getId()))
                .collect(Collectors.groupingBy(
                        u -> Optional.ofNullable(u.getOutlet()).orElse("Unknown"),
                        Collectors.counting()
                ));

        // Most active region/outlet
        SurveyActivityStatsDTO.RegionActivityDTO mostActiveRegion =
                regionResponded.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(e -> new SurveyActivityStatsDTO.RegionActivityDTO(e.getKey(), e.getValue().intValue()))
                        .orElse(null);

        SurveyActivityStatsDTO.OutletActivityDTO mostActiveOutlet =
                outletResponded.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(e -> new SurveyActivityStatsDTO.OutletActivityDTO(e.getKey(), e.getValue().intValue()))
                        .orElse(null);

        // Least active region/outlet
        SurveyActivityStatsDTO.RegionActivityDTO leastActiveRegion =
                regionNonResponded.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(e -> new SurveyActivityStatsDTO.RegionActivityDTO(e.getKey(), e.getValue().intValue()))
                        .orElse(null);

        SurveyActivityStatsDTO.OutletActivityDTO leastActiveOutlet =
                outletNonResponded.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(e -> new SurveyActivityStatsDTO.OutletActivityDTO(e.getKey(), e.getValue().intValue()))
                        .orElse(null);

        return SurveyActivityStatsDTO.builder()
                .mostActiveRegions(mostActiveRegion)
                .leastActiveRegions(leastActiveRegion)
                .mostActiveOutlets(mostActiveOutlet)
                .leastActiveOutlets(leastActiveOutlet)
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
