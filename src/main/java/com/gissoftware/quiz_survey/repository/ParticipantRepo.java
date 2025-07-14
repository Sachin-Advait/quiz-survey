package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.ParticipantModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ParticipantRepo extends MongoRepository<ParticipantModel, String> {
    Optional<ParticipantModel> findByQuizSurveyIdAndExternalUserId(String quizSurveyId, String externalUserId);
}
