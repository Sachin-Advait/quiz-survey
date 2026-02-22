package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.UserModel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<UserModel, String> {

    // 🔎 Find active user by staffId
    Optional<UserModel> findByStaffIdAndActiveUserTrue(String staffId);

    // 📄 Paging queries (active users only)
    Page<UserModel> findByRegionAndOutletAndActiveUserTrue(String region, String outlet, Pageable pageable);

    Page<UserModel> findByRegionAndActiveUserTrue(String region, Pageable pageable);

    Page<UserModel> findByOutletAndActiveUserTrue(String outlet, Pageable pageable);

    // 📋 List queries (active users only)
    List<UserModel> findByRegionAndActiveUserTrue(String region);

    List<UserModel> findByOutletAndActiveUserTrue(String outlet);

    List<UserModel> findByRegionAndOutletAndActiveUserTrue(String region, String outlet);

    // 🗺️ Distinct regions from active users only
    @Query(value = "{ 'activeUser': true }", fields = "{ 'region' : 1 }")
    List<UserModel> findAllRegionsOfActiveUsers();
}