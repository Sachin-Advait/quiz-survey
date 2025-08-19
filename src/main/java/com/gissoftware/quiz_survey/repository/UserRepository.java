package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<UserModel, String> {
    Optional<UserModel> findByStaffId(String staffId);

    Page<UserModel> findByRegionAndOutlet(String region, String outlet, Pageable pageable);

    Page<UserModel> findByRegion(String region, Pageable pageable);

    Page<UserModel> findByOutlet(String outlet, Pageable pageable);

    List<UserModel> findByRegion(String region);

    List<UserModel> findByOutlet(String outlet);

    List<UserModel> findByRegionAndOutlet(String region, String outlet);
}

