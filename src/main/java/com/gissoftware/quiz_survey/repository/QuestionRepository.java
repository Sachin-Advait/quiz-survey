package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.QuestionModel;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface QuestionRepository extends MongoRepository<QuestionModel, String> {
}
