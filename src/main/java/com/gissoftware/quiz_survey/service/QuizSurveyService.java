package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.QuizSurveyDTO;
import com.gissoftware.quiz_survey.dto.QuizzesSurveysDTO;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizSurveyService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final ResponseRepo responseRepo;
    private final UserRepository userRepository;

    // Create Quiz & Survey
    public QuizSurveyModel createQuizSurvey(QuizSurveyModel model) {
        return quizSurveyRepo.save(model);
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
                .build();
    }

    // Get All Quizzes and Surveys
    public List<QuizzesSurveysDTO> getQuizzesSurveys(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        UserModel user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Invalid username"));

        List<QuizSurveyModel> quizzes = quizSurveyRepo.findAll().stream()
                .filter(quiz ->
                        quiz.getTargetedUsers() != null &&
                                !quiz.getTargetedUsers().isEmpty() &&
                                quiz.getTargetedUsers().contains(user.getUsername())
                )
                .toList();

        return quizzes.stream().map(quiz -> {
            int totalQuestions = quiz.getDefinitionJson().getPages() != null &&
                    !quiz.getDefinitionJson().getPages().isEmpty() &&
                    quiz.getDefinitionJson().getPages().get(0).getElements() != null
                    ? quiz.getDefinitionJson().getPages().get(0).getElements().size()
                    : 0;

            boolean isParticipated = responseRepo.findByQuizSurveyIdAndUserId(quiz.getId(), userId).isPresent();
            boolean isMandatory = !isParticipated && Boolean.TRUE.equals(quiz.getIsMandatory());

            return QuizzesSurveysDTO.builder()
                    .id(quiz.getId())
                    .type(quiz.getType())
                    .title(quiz.getTitle())
                    .totalQuestion(totalQuestions)
                    .status(quiz.getStatus())
                    .quizTotalDuration(quiz.getQuizTotalDuration())
                    .isAnnounced(quiz.getIsAnnounced())
                    .createdAt(quiz.getCreatedAt())
                    .isParticipated(isParticipated)
                    .isMandatory(isMandatory)
                    .build();
        }).toList();
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
        if (model.getTargetedUsers() != null) existing.setTargetedUsers(model.getTargetedUsers());


        return quizSurveyRepo.save(existing);
    }

    // Delete Quiz & Survey by ID
    public void deleteQuizSurvey(String id) {
        quizSurveyRepo.deleteById(id);
    }
}
