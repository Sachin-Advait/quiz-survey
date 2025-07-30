package com.gissoftware.quiz_survey.repository;


import com.gissoftware.quiz_survey.model.FCMToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FCMTokenRepository extends MongoRepository<FCMToken, String> {

    List<FCMToken> findByUserId(String userId);

    void deleteByUserId(String userId);
}
