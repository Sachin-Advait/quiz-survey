package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.TrainingMaterial;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingMaterialRepository extends MongoRepository<TrainingMaterial, String> {

  List<TrainingMaterial> findByRegion(String region);

  List<TrainingMaterial> findByRegionIn(List<String> regions);
}
