package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizSurveyRepository extends MongoRepository<QuizSurveyModel, String> {
}
