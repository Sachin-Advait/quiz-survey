package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.UserModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<UserModel, String> {
    Optional<UserModel> findByUsername(String username);

    List<UserModel> findByUsernameIn(List<String> usernames);

    List<UserModel> findByRegion(String region);

    List<UserModel> findByOutlet(String outlet);

    List<UserModel> findByRegionAndOutlet(String region, String outlet);

}

