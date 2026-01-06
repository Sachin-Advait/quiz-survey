package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.TrainingMaterial;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingMaterialRepository extends MongoRepository<TrainingMaterial, String> {

  List<TrainingMaterial> findAllByActiveTrue();

  List<TrainingMaterial> findByRegionAndActiveTrue(String region);

  boolean existsByCloudinaryPublicIdAndActiveTrue(String cloudinaryPublicId);

  List<TrainingMaterial> findByCloudinaryResourceTypeAndDurationAndActiveTrue(
      String cloudinaryResourceType, String duration);

  Optional<TrainingMaterial> findByIdAndActiveTrue(String id);
}
