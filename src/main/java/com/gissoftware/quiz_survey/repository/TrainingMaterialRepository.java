package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.TrainingMaterial;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingMaterialRepository extends MongoRepository<TrainingMaterial, String> {

  // ✅ Active trainings
  List<TrainingMaterial> findAllByActiveTrue();

  // ✅ Region filter
  List<TrainingMaterial> findByRegionAndActiveTrue(String region);

  // ✅ Provider-agnostic (Cloudinary / Bunny / S3 / Vimeo)
  boolean existsByVideoPublicIdAndActiveTrue(String videoPublicId);

  // ✅ Filter by provider + duration
  List<TrainingMaterial> findByVideoProviderAndDurationAndActiveTrue(
      String videoProvider, String duration);

  // ✅ Safe fetch (ignores deleted)
  Optional<TrainingMaterial> findByIdAndActiveTrue(String id);
}
