package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.QuizSurveyVersionModel;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizSurveyVersionRepository
    extends MongoRepository<QuizSurveyVersionModel, String> {
  List<QuizSurveyVersionModel> findByQuizSurveyIdOrderByVersionAsc(String quizSurveyId);
}
