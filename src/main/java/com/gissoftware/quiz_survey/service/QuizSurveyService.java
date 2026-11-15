package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.Utils.ScoringUtil;
import com.gissoftware.quiz_survey.Utils.UserDataFieldConstants;
import com.gissoftware.quiz_survey.controller.QuizSurveySseController;
import com.gissoftware.quiz_survey.dto.PageResponseDTO;
import com.gissoftware.quiz_survey.dto.QuizScoreSummaryDTO;
import com.gissoftware.quiz_survey.dto.QuizSurveyDTO;
import com.gissoftware.quiz_survey.dto.QuizzesSurveysDTO;
import com.gissoftware.quiz_survey.mapper.QuizSurveyMapper;
import com.gissoftware.quiz_survey.model.AnnouncementMode;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.QuizSurveyVersionModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.VisibilityType;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.QuizSurveyVersionRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuizSurveyService {
  private static final Logger log = LoggerFactory.getLogger(QuizSurveyService.class);

  private final QuizSurveyRepository quizSurveyRepo;
  private final UserRepository userRepository;
  private final QuizSurveyMapper quizSurveyMapper;
  private final ResponseRepo responseRepo;
  private final FCMService fcmService;
  private final MongoTemplate mongoTemplate;
  private final QuizSurveySseController quizSurveySseController;
  private final QuizSurveyVersionRepository quizSurveyVersionRepo;

  // Get Quiz & Survey By ID
  public QuizSurveyDTO getQuizSurvey(String id, String userId) {

    QuizSurveyModel quiz =
        quizSurveyRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Quiz or survey not found"));

    ResponseModel response =
        responseRepo.findByQuizSurveyIdAndUserId(id, userId).stream()
            .filter(r -> r.getScore() == null && r.getAnswers() == null)
            .max(
                Comparator.comparing(
                    ResponseModel::getOpenedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .orElse(null);

    if (response == null) {

      response =
          ResponseModel.builder().quizSurveyId(id).userId(userId).openedAt(Instant.now()).build();

      responseRepo.save(response);

    } else {

      response.setOpenedAt(Instant.now());
      responseRepo.save(response);
    }

    return QuizSurveyDTO.builder()
        .id(quiz.getId())
        .type(quiz.getType())
        .title(quiz.getTitle())
        .definitionJson(quiz.getDefinitionJson())
        .quizDuration(quiz.getQuizDuration())
        .quizTotalDuration(quiz.getQuizTotalDuration())
        .maxScore(quiz.getMaxScore())
        .answerKey(quiz.getAnswerKey())
        .createdAt(quiz.getCreatedAt())
        .maxRetake(quiz.getMaxRetake())
        .visibilityType(quiz.getVisibilityType())
        .announcementMode(quiz.getAnnouncementMode())
        .userDataDisplayFields(quiz.getUserDataDisplayFields())
        .build();
  }

  public Page<QuizSurveyModel> findWithFilters(
      String userId,
      String status,
      String type,
      String participation,
      Instant startDate,
      Pageable pageable) {
    Query query = new Query();

    // Core user filtering logic (preserving original behavior)
    if (userId != null) {
      // Validate user exists (preserving original validation)
      userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Invalid username"));

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
    List<QuizSurveyModel> allResults;
    // Execute the query first to get eligible quizzes
    allResults = mongoTemplate.find(Query.of(query).limit(-1).skip(-1), QuizSurveyModel.class);

    // Apply participation filter if needed
    if (userId != null && participation != null && !participation.equalsIgnoreCase("All")) {
      // Get all quiz IDs that the user has responded to
      List<ResponseModel> userResponses = responseRepo.findByUserId(userId);
      Set<String> participatedQuizIds =
          userResponses.stream().map(ResponseModel::getQuizSurveyId).collect(Collectors.toSet());

      if (participation.equalsIgnoreCase("Participated")) {
        // Filter to show only quizzes where user has submitted responses
        allResults =
            allResults.stream()
                .filter(quiz -> participatedQuizIds.contains(quiz.getId()))
                .collect(Collectors.toList());

      } else if (participation.equalsIgnoreCase("Not Participated")) {
        // Filter to show only quizzes where user has NOT submitted responses
        allResults =
            allResults.stream()
                .filter(quiz -> !participatedQuizIds.contains(quiz.getId()))
                .collect(Collectors.toList());
      }
    }

    // Apply sorting
    if (pageable.getSort().isSorted()) {
      allResults =
          allResults.stream()
              .sorted(
                  (q1, q2) -> {
                    for (Sort.Order order : pageable.getSort()) {
                      int comparison = 0;
                      if ("createdAt".equals(order.getProperty())) {
                        comparison = q1.getCreatedAt().compareTo(q2.getCreatedAt());
                      }

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

    List<QuizSurveyModel> pagedResults =
        start < allResults.size() ? allResults.subList(start, end) : Collections.emptyList();

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
      int size) {
    Sort.Direction direction =
        (sort != null && sort.equalsIgnoreCase("Oldest"))
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

    Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));

    Page<QuizSurveyModel> pagedQuizzes =
        findWithFilters(userId, status, type, participation, startDate, pageable);

    List<QuizzesSurveysDTO> result =
        pagedQuizzes.getContent().stream()
            .map(
                q ->
                    userId == null
                        ? quizSurveyMapper.mapToDtoWithoutUser(q)
                        : quizSurveyMapper.mapToDtoWithUser(q, userId))
            .toList();

    Page<QuizzesSurveysDTO> dtoPage =
        new PageImpl<>(result, pageable, pagedQuizzes.getTotalElements());

    return new PageResponseDTO<>(dtoPage);
  }

  // Create Quiz & Survey
  public QuizSurveyModel createQuizSurvey(QuizSurveyModel model) {

    if (model.getVisibilityType() == null) {
      throw new IllegalArgumentException("Visibility not defined!");
    }

    if (model.getVisibilityType() == VisibilityType.PRIVATE
        && !model.getUserDataDisplayFields().isEmpty()) {

      List<String> filteredFields =
          model.getUserDataDisplayFields().stream()
              .filter(UserDataFieldConstants.ALLOWED_FIELDS::contains)
              .distinct()
              .toList();

      model.setUserDataDisplayFields(filteredFields);
    } else if (model.getVisibilityType() == VisibilityType.PRIVATE) {
      throw new IllegalArgumentException("User field are empty");
    }

    model.setIsAnnounced(model.getAnnouncementMode() == AnnouncementMode.IMMEDIATE);
    model.setVersion(1);
    QuizSurveyModel savedQuiz = quizSurveyRepo.save(model);
    quizSurveySseController.pushNewSurvey(savedQuiz);

    fcmService.notifyQuizSurveyAssigned(savedQuiz);
    return savedQuiz;
  }

  // Update Quiz & Survey by ID
  public QuizSurveyModel updateQuizSurvey(QuizSurveyModel model) {
    QuizSurveyModel existing =
        quizSurveyRepo
            .findById(model.getId())
            .orElseThrow(() -> new IllegalArgumentException("Quiz/Survey not found"));

    if (model.getTitle() != null) existing.setTitle(model.getTitle());
    if (model.getType() != null) existing.setType(model.getType());
    if (model.getDefinitionJson() != null) existing.setDefinitionJson(model.getDefinitionJson());
    if (model.getAnswerKey() != null) existing.setAnswerKey(model.getAnswerKey());
    if (model.getMaxScore() != null) existing.setMaxScore(model.getMaxScore());
    if (model.getStatus() != null) existing.setStatus(model.getStatus());
    if (model.getQuizTotalDuration() != null)
      existing.setQuizTotalDuration(model.getQuizTotalDuration());
    if (model.getQuizDuration() != null) existing.setQuizDuration(model.getQuizDuration());
    if (model.getIsAnnounced() != null) existing.setIsAnnounced(model.getIsAnnounced());
    if (model.getIsMandatory() != null) existing.setIsMandatory(model.getIsMandatory());
    if (!model.getTargetedUsers().isEmpty()) existing.setTargetedUsers(model.getTargetedUsers());
    if (model.getMaxRetake() != null) existing.setMaxRetake(model.getMaxRetake());
    if (!model.getUserDataDisplayFields().isEmpty())
      existing.setUserDataDisplayFields(model.getUserDataDisplayFields());

    return quizSurveyRepo.save(existing);
  }

  /**
   * Edit a published quiz/survey in place (same ID), so every earlier completion stays linked to
   * it and stays in its reports.
   *
   * <p>completedUsersPolicy decides what happens to submissions made on the previous version:
   * KEEP (default) leaves them untouched; RESCORE re-marks quiz answers against the new answer key
   * and keeps the old score in originalScore.
   */
  public QuizSurveyModel editQuizSurvey(
      String id, QuizSurveyModel model, String completedUsersPolicy) {
    QuizSurveyModel existing =
        quizSurveyRepo
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Quiz/Survey not found"));

    String policy =
        completedUsersPolicy == null ? "KEEP" : completedUsersPolicy.trim().toUpperCase();
    if (!policy.equals("KEEP") && !policy.equals("RESCORE")) {
      throw new IllegalArgumentException("Invalid completedUsersPolicy: " + completedUsersPolicy);
    }
    boolean isQuiz = "quiz".equalsIgnoreCase(existing.getType());
    if (policy.equals("RESCORE") && !isQuiz) {
      throw new IllegalArgumentException("Re-score is only available for quizzes");
    }

    int previousVersion = existing.getVersion() != null ? existing.getVersion() : 1;
    Instant now = Instant.now();

    // 1. Snapshot the questions being replaced
    quizSurveyVersionRepo.save(
        QuizSurveyVersionModel.builder()
            .quizSurveyId(id)
            .version(previousVersion)
            .title(existing.getTitle())
            .definitionJson(existing.getDefinitionJson())
            .answerKey(existing.getAnswerKey())
            .maxScore(existing.getMaxScore())
            .completedUsersPolicy(policy)
            .replacedAt(now)
            .build());

    // 2. Tag responses submitted before versioning existed with the version they were taken on
    mongoTemplate.updateMulti(
        new Query(Criteria.where("quizSurveyId").is(id).and("quizVersion").exists(false)),
        new Update().set("quizVersion", previousVersion),
        ResponseModel.class);

    // 3. Never drop an already-targeted user, so their completion stays in the report
    List<String> previousTargets = new ArrayList<>(existing.getTargetedUsers());
    if (model.getTargetedUsers() != null && !model.getTargetedUsers().isEmpty()) {
      LinkedHashSet<String> merged = new LinkedHashSet<>(previousTargets);
      merged.addAll(model.getTargetedUsers());
      model.setTargetedUsers(new ArrayList<>(merged));
    }

    // 4. Apply the edit to the same document
    model.setId(id);
    model.setIsAnnounced(null); // editing never changes announcement state
    QuizSurveyModel updated = updateQuizSurvey(model);
    updated.setVersion(previousVersion + 1);
    updated.setUpdatedAt(now);
    updated = quizSurveyRepo.save(updated);

    // 5. Re-score earlier quiz submissions if asked
    List<ResponseModel> submitted =
        responseRepo.findByQuizSurveyId(id).stream().filter(r -> r.getAnswers() != null).toList();

    if (policy.equals("RESCORE")) {
      Map<String, String> questionTypes = new HashMap<>();
      Map<String, Integer> questionMarks = new HashMap<>();
      if (updated.getDefinitionJson() != null && updated.getDefinitionJson().getPages() != null) {
        updated
            .getDefinitionJson()
            .getPages()
            .forEach(
                page -> {
                  if (page.getElements() == null) return;
                  page.getElements()
                      .forEach(
                          el -> {
                            questionTypes.put(el.getName(), el.getType());
                            questionMarks.put(
                                el.getName(), el.getMarks() != null ? el.getMarks() : 1);
                          });
                });
      }
      Map<String, Object> answerKey = new HashMap<>();
      if (updated.getAnswerKey() != null) {
        // only score questions that still exist
        updated.getAnswerKey().forEach(
            (k, v) -> {
              if (questionMarks.containsKey(k) && v != null) answerKey.put(k, v);
            });
      }

      List<ResponseModel> rescored = new ArrayList<>();
      for (ResponseModel r : submitted) {
        if (r.getScore() == null) continue;
        int newScore =
            ScoringUtil.score(r.getAnswers(), answerKey, questionTypes, questionMarks).score();
        if (r.getOriginalScore() == null) r.setOriginalScore(r.getScore());
        r.setScore(newScore);
        r.setMaxScore(updated.getMaxScore());
        r.setRescoredAt(now);
        rescored.add(r);
      }
      responseRepo.saveAll(rescored);
    }

    // 6. Notify only targeted users who have not completed it yet
    Set<String> completedUserIds =
        submitted.stream().map(ResponseModel::getUserId).collect(Collectors.toSet());
    List<String> pendingUsers =
        updated.getTargetedUsers().stream().filter(u -> !completedUserIds.contains(u)).toList();
    if (!pendingUsers.isEmpty()) {
      fcmService.notifyQuizSurveyAssigned(
          QuizSurveyModel.builder()
              .id(updated.getId())
              .type(updated.getType())
              .title(updated.getTitle())
              .targetedUsers(pendingUsers)
              .build());
    }

    return updated;
  }

  // Delete Quiz & Survey by ID
  public void deleteQuizSurvey(String id) {
    quizSurveyRepo.deleteById(id);
  }

  public QuizScoreSummaryDTO quizScoreSummary(String quizSurveyId) {
    List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);
    if (responses.isEmpty()) {
      throw new IllegalArgumentException("No responses found for quiz/survey: " + quizSurveyId);
    }

    // ✅ Pick only the best attempt per user
    Map<String, ResponseModel> bestAttempts =
        responses.stream()
            .filter(r -> r.getScore() != null)
            .collect(
                Collectors.toMap(
                    ResponseModel::getUserId,
                    r -> r,
                    (r1, r2) -> r1.getScore() >= r2.getScore() ? r1 : r2));

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

    List<ResponseModel> sorted =
        uniqueResponses.stream()
            .filter(r -> r.getScore() != null)
            .sorted(
                Comparator.comparing(ResponseModel::getScore, Comparator.reverseOrder())
                    .thenComparing(
                        ResponseModel::getFinishTime,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(
                        ResponseModel::getSubmittedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();

    // ✅ Get only top 3 (after proper tie-breaking)
    List<Map<String, Object>> topScorers =
        sorted.stream()
            .limit(3)
            .map(
                r -> {
                  Map<String, Object> map = new HashMap<>();
                  map.put("username", r.getUsername());
                  map.put("userId", r.getUserId());
                  map.put("score", r.getScore());
                  map.put("maxScore", r.getMaxScore());
                  map.put("finishTime", r.getFinishTime());
                  map.put("submittedAt", r.getSubmittedAt());
                  return map;
                })
            .collect(Collectors.toList());

    return new QuizScoreSummaryDTO(totalAttempts, avgScore, highestScore, maxScore, topScorers);
  }

  public QuizSurveyModel getById(String id) {
    return quizSurveyRepo
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Quiz/Survey not found"));
  }
}
