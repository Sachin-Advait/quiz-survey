package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.UserGroupModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserGroupRepository extends MongoRepository<UserGroupModel, String> {}
