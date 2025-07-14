package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.model.ParticipantModel;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.repository.ParticipantRepo;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuizSurveyService {

    private final QuizSurveyRepository quizSurveyRepo;
    private final ParticipantRepo participantRepo;
    private final ResponseRepo responseRepo;

    public QuizSurveyModel getQuizSurvey(String id) {
        return quizSurveyRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Quiz or survey not found"));
    }

    @Transactional
    public ResponseModel storeResponse(String quizSurveyId, String externalUserId, Map<String, Object> userAnswers) {
        QuizSurveyModel qs = getQuizSurvey(quizSurveyId);

        ParticipantModel participant = participantRepo
                .findByQuizSurveyIdAndExternalUserId(quizSurveyId, externalUserId)
                .orElseGet(() -> participantRepo.save(
                        ParticipantModel.builder()
                                .quizSurveyId(quizSurveyId)
                                .externalUserId(externalUserId)
                                .build()
                ));

        Integer score = null, max = null;
        if ("quiz".equalsIgnoreCase(qs.getType())) {
            ScoringUtil.ScoringResult result = ScoringUtil.score(userAnswers, qs.getAnswerKey());
            score = result.score();
            max = result.max();
        }

        return responseRepo.save(ResponseModel.builder()
                .quizSurveyId(quizSurveyId)
                .participantId(participant.getId())
                .answers(userAnswers)
                .score(score)
                .maxScore(max)
                .build());
    }
}
