package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.ResponseModel;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResponseRepo extends MongoRepository<ResponseModel, String> {
    long countByQuizSurveyId(String quizSurveyId);
}

