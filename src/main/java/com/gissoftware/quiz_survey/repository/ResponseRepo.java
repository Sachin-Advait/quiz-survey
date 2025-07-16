package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.ResponseModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ResponseRepo extends MongoRepository<ResponseModel, String> {
    long countByQuizSurveyId(String quizSurveyId);

    List<ResponseModel> findByQuizSurveyId(String quizSurveyId);

    List<ResponseModel> findByUserId(String userId);

}

