package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.QuizSurveyDTO;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizSurveyService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final ResponseRepo responseRepo;

    public QuizSurveyModel getQuizSurvey(String id) {
        return quizSurveyRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz or survey not found"));
    }

    public QuizSurveyModel createQuizSurvey(QuizSurveyModel model) {
        return quizSurveyRepo.save(model);
    }

    // Get All Quizzes and Surveys
    public List<QuizSurveyDTO> getQuizzesSurveys(String userId) {
        List<QuizSurveyModel> quizzes = quizSurveyRepo.findAll();

        return quizzes.stream().map(quiz -> {
            QuizSurveyDTO.QuizSurveyDTOBuilder builder = QuizSurveyDTO.builder()
                    .id(quiz.getId())
                    .type(quiz.getType())
                    .title(quiz.getTitle())
                    .definitionJson(quiz.getDefinitionJson())
                    .answerKey(quiz.getAnswerKey())
                    .maxScore(quiz.getMaxScore())
                    .status(quiz.getStatus())
                    .quizTotalDuration(quiz.getQuizTotalDuration())
                    .isAnnounced(quiz.getIsAnnounced())
                    .createdAt(quiz.getCreatedAt());

            if (userId != null) {
                builder.isParticipated(
                        responseRepo.findByQuizSurveyIdAndUserId(quiz.getId(), userId).isPresent()
                );
            }

            return builder.build();
        }).toList();
    }

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
        if (model.getIsAnnounced() != null) existing.setIsAnnounced(model.getIsAnnounced());

        return quizSurveyRepo.save(existing);
    }


    public void deleteQuizSurvey(String id) {
        quizSurveyRepo.deleteById(id);
    }

}
