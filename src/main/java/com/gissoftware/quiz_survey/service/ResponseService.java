package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.Utils.ScoringUtil;
import com.gissoftware.quiz_survey.dto.LowScoringUserDTO;
import com.gissoftware.quiz_survey.dto.ResponseReceivedDTO;
import com.gissoftware.quiz_survey.dto.SurveySubmissionRequest;
import com.gissoftware.quiz_survey.dto.UserResponseDTO;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@RequiredArgsConstructor
public class ResponseService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final ResponseRepo responseRepo;
    private final UserRepository userRepository;

    private final MongoTemplate mongoTemplate;

    // Store Quiz & Survey Responses
    // @Transactional
    public ResponseModel storeResponse(String quizSurveyId, SurveySubmissionRequest request) {
        UserModel user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Invalid username"));

        // Fetch quiz/survey
        QuizSurveyModel qs = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new IllegalArgumentException("Quiz or survey not found"));

        boolean userExists = qs.getTargetedUsers().stream()
                .anyMatch(s -> s.contains(user.getId()));

        if (!userExists) {
            throw new IllegalArgumentException("User does not exist in the target users");
        }

        List<ResponseModel> existingResponses = responseRepo.findByQuizSurveyIdAndUserId(quizSurveyId, request.getUserId());

        // ✅ Check if the user has already submitted a response
        if (qs.getType().equalsIgnoreCase("survey")) {
            if (!existingResponses.isEmpty()) {
                throw new IllegalStateException("You have already submitted this survey.");
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
        Map<String, Object> given = request.getAnswers();
        Map<String, Object> answerKey = quiz.getAnswerKey();

        Map<String, String> questionTypes = new HashMap<>();
        Map<String, Integer> questionMarks = new HashMap<>();

        quiz.getDefinitionJson().getPages().forEach(page ->
                page.getElements().forEach(el -> {
                    questionTypes.put(el.getName(), el.getType());
                    questionMarks.put(el.getName(), el.getMarks() != null ? el.getMarks() : 1); // default mark = 1
                })
        );

        ScoringUtil.ScoringResult result = ScoringUtil.score(given, answerKey, questionTypes, questionMarks);

        UserModel user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("Invalid userId"));

        quiz.setMaxRetake(quiz.getMaxRetake() - 1);
        quizSurveyRepo.save(quiz);


        return responseRepo.save(ResponseModel.builder()
                .quizSurveyId(quiz.getId())
                .userId(request.getUserId())
                .username(user.getUsername())
                .answers(request.getAnswers())
                .score(result.score())
                .maxScore(quiz.getMaxScore())
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

    // Get All Responses by User ID
    public List<ResponseModel> getAllResponsesByUserId(String userId) {
        return responseRepo.findByUserId(userId);
    }

    public List<UserResponseDTO> totalStaffInvited(String quizSurveyId) {
        QuizSurveyModel quiz = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));

        List<UserModel> users = userRepository.findAllById(quiz.getTargetedUsers());

        List<String> userVisibleField = quiz.getUserDataDisplayFields();

        return users.stream()
                .map(user -> {
                    UserResponseDTO.UserResponseDTOBuilder builder = UserResponseDTO.builder();

                    builder.id(user.getId());

                    if (userVisibleField.contains("staffId")) {
                        builder.staffId(user.getStaffId());
                    }
                    if (userVisibleField.contains("username")) {
                        builder.username(user.getUsername());
                    }
                    if (userVisibleField.contains("role")) {
                        builder.role(user.getRole());
                    }
                    if (userVisibleField.contains("region")) {
                        builder.region(user.getRegion());
                    }
                    if (userVisibleField.contains("outlet")) {
                        builder.outlet(user.getOutlet());
                    }
                    if (userVisibleField.contains("position")) {
                        builder.position(user.getPosition());
                    }

                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    public List<ResponseReceivedDTO> totalResponseReceived(String quizSurveyId) {
        QuizSurveyModel quiz = quizSurveyRepo.findById(quizSurveyId)
                .orElseThrow(() -> new IllegalArgumentException("Survey not found"));

        List<String> userVisibleField = quiz.getUserDataDisplayFields();

        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(quizSurveyId);

        Map<String, ResponseModel> highestScoreResponseMap = responses.stream()
                .collect(Collectors.toMap(
                        ResponseModel::getUserId,
                        response -> response,
                        (r1, r2) -> r1.getScore() >= r2.getScore() ? r1 : r2
                ));

        List<UserModel> users = userRepository.findAllById(responses.stream().map(ResponseModel::getUserId).toList());

        return users.stream()
                .map(user -> {
                    ResponseModel response = highestScoreResponseMap.get(user.getId());

                    int score = (response.getScore() != null) ? response.getScore() : 0;
                    int maxScore = (response.getMaxScore() != null) ? response.getMaxScore() : 100;

                    String result = (score >= 0.5 * maxScore) ? "PASS" : "FAIL";

                    ResponseReceivedDTO.ResponseReceivedDTOBuilder builder = ResponseReceivedDTO.builder();

                    // Always include ID for identification purposes
                    builder.id(user.getId());

                    builder.result(result);
                    builder.submittedAt(response.getSubmittedAt());

                    if (userVisibleField.contains("staffId")) {
                        builder.staffId(user.getStaffId());
                    }
                    if (userVisibleField.contains("username")) {
                        builder.username(user.getUsername());
                    }
                    if (userVisibleField.contains("role")) {
                        builder.role(user.getRole());
                    }
                    if (userVisibleField.contains("region")) {
                        builder.region(user.getRegion());
                    }
                    if (userVisibleField.contains("outlet")) {
                        builder.outlet(user.getOutlet());
                    }
                    if (userVisibleField.contains("position")) {
                        builder.position(user.getPosition());
                    }

                    return builder.build();

                })
                .collect(Collectors.toList());
    }

    public List<LowScoringUserDTO> getLowScoringUsers(int weeks, double thresholdPercent) {
        Instant fromDate = Instant.now().minus(weeks * 7L, ChronoUnit.DAYS);

        MatchOperation match = match(
                Criteria.where("submittedAt").gte(fromDate)
                        .and("score").ne(null)
                        .and("maxScore").gt(0)
        );

        AddFieldsOperation addPercentage = addFields()
                .addField("percentage")
                .withValue(
                        ArithmeticOperators.Multiply.valueOf(
                                ArithmeticOperators.Divide.valueOf("$score").divideBy("$maxScore")
                        ).multiplyBy(100)
                ).build();

        GroupOperation groupByUser = group("userId")
                .avg("percentage").as("avgPercentage")
                .push("quizSurveyId").as("attemptedQuizzes")
                .count().as("attemptCount")
                .first("userId").as("userId")
                .first("username").as("username");

        AddFieldsOperation convertUserId = addFields()
                .addField("userIdObj")
                .withValue(ConvertOperators.ToObjectId.toObjectId("$userId"))
                .build();

        LookupOperation lookupUser = LookupOperation.newLookup()
                .from("users")
                .localField("userIdObj")
                .foreignField("_id")
                .as("userDetails");

        UnwindOperation unwindUser = unwind("userDetails");

        MatchOperation avgBelowThreshold = match(Criteria.where("avgPercentage").lt(thresholdPercent));

        SortOperation sortByLowest = sort(Sort.by(Sort.Direction.ASC, "avgPercentage"));

        ProjectionOperation project = project()
                .and("userId").as("userId")
                .and("userDetails.staffId").as("staffId")
                .and("username").as("username")
                .and("avgPercentage").as("avgPercentage")
                .and("attemptedQuizzes").as("attemptedQuizzes")
                .and(ConvertOperators.ToLong.toLong("$attemptCount")).as("attemptCount")
                .and("userDetails.region").as("region")
                .and("userDetails.outlet").as("outlet");

        Aggregation aggregation = newAggregation(
                match,
                addPercentage,
                groupByUser,
                avgBelowThreshold,
                sortByLowest,
                convertUserId,
                lookupUser,
                unwindUser,
                project
        );

        return mongoTemplate.aggregate(aggregation, "responses", LowScoringUserDTO.class).getMappedResults();
    }
}