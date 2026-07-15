package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.QuizAttempt;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizAttemptRepository extends MongoRepository<QuizAttempt, String> {

  List<QuizAttempt> findByQuizSurveyIdAndUserId(String quizSurveyId, String userId);

}