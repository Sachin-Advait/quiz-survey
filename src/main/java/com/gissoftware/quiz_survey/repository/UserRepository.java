package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.UserModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserModel, String> {
    Optional<UserModel> findByUsername(String userId);
}

