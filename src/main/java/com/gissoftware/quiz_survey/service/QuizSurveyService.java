package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.Utils.UserDataFieldConstants;
import com.gissoftware.quiz_survey.controller.QuizSurveySocketController;
import com.gissoftware.quiz_survey.dto.PageResponseDTO;
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
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class QuizSurveyService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final UserRepository userRepository;
    private final QuizSurveySocketController quizSurveySocketController;
    private final QuizSurveyMapper quizSurveyMapper;
    private final ResponseRepo responseRepo;

    private final MongoTemplate mongoTemplate;

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

    public Page<QuizSurveyModel> findWithFilters(
            String userId,
            String status,
            String type,
            String participation,
            Instant startDate,
            Pageable pageable
    ) {
        Query query = new Query();

        // Core user filtering logic (preserving original behavior)
        if (userId != null) {
            // Validate user exists (preserving original validation)
            userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Invalid username"));

            // Always filter to show only quizzes where user is targeted (preserving original logic)
            query.addCriteria(Criteria.where("targetedUsers").in(userId));
        }

        // Additional filters (new functionality)

        // Status filter
        if (status != null && !status.equalsIgnoreCase("All Status")) {
            boolean active = status.equalsIgnoreCase("Active");
            query.addCriteria(Criteria.where("status").is(active));
        }

        // Type filter
        if (type != null && !type.equalsIgnoreCase("All Types")) {
            query.addCriteria(Criteria.where("type").regex(type, "i"));
        }

        // Date range
        if (startDate != null) {
            Instant endDate = startDate.plus(1, ChronoUnit.DAYS);

            query.addCriteria(Criteria.where("createdAt").gte(startDate).lt(endDate));
        }

        // Execute the query first to get eligible quizzes
        List<QuizSurveyModel> allResults = mongoTemplate.find(Query.of(query).limit(-1).skip(-1), QuizSurveyModel.class);

        // Apply participation filter if needed
        if (userId != null && participation != null && !participation.equalsIgnoreCase("All")) {
            // Get all quiz IDs that the user has responded to
            List<ResponseModel> userResponses = responseRepo.findByUserId(userId);
            Set<String> participatedQuizIds = userResponses.stream()
                    .map(ResponseModel::getQuizSurveyId)
                    .collect(Collectors.toSet());

            if (participation.equalsIgnoreCase("Participated")) {
                // Filter to show only quizzes where user has submitted responses
                allResults = allResults.stream()
                        .filter(quiz -> participatedQuizIds.contains(quiz.getId()))
                        .collect(Collectors.toList());
            } else if (participation.equalsIgnoreCase("Not Participated")) {
                // Filter to show only quizzes where user has NOT submitted responses
                allResults = allResults.stream()
                        .filter(quiz -> !participatedQuizIds.contains(quiz.getId()))
                        .collect(Collectors.toList());
            }
        }

        // Apply sorting
        if (pageable.getSort().isSorted()) {
            allResults = allResults.stream()
                    .sorted((q1, q2) -> {
                        for (Sort.Order order : pageable.getSort()) {
                            int comparison = 0;
                            if ("createdAt".equals(order.getProperty())) {
                                comparison = q1.getCreatedAt().compareTo(q2.getCreatedAt());
                            }
                            // Add other sortable fields as needed

                            if (order.getDirection() == Sort.Direction.DESC) {
                                comparison = -comparison;
                            }

                            if (comparison != 0) {
                                return comparison;
                            }
                        }
                        return 0;
                    })
                    .collect(Collectors.toList());
        }

        // Apply pagination manually
        long total = allResults.size();
        int start = (int) ((long) pageable.getPageNumber() * pageable.getPageSize());
        int end = Math.min(start + pageable.getPageSize(), allResults.size());

        List<QuizSurveyModel> pagedResults = start < allResults.size()
                ? allResults.subList(start, end)
                : Collections.emptyList();

        return new PageImpl<>(pagedResults, pageable, total);
    }

    // Updated main method
    public PageResponseDTO<QuizzesSurveysDTO> getQuizzesSurveys(
            String userId,
            String status,
            String type,
            String sort,
            String participation,
            Instant startDate,
            int page,
            int size
    ) {
        Sort.Direction direction = (sort != null && sort.equalsIgnoreCase("Oldest"))
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

        Page<QuizSurveyModel> pagedQuizzes = findWithFilters(
                userId, status, type, participation, startDate, pageable
        );

        List<QuizzesSurveysDTO> result = pagedQuizzes.getContent().stream()
                .map(q -> userId == null
                        ? quizSurveyMapper.mapToDtoWithoutUser(q)
                        : quizSurveyMapper.mapToDtoWithUser(q, userId))
                .toList();

        Page<QuizzesSurveysDTO> dtoPage = new PageImpl<>(result, pageable, pagedQuizzes.getTotalElements());

        return new PageResponseDTO<>(dtoPage);
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
