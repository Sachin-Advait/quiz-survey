package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.TrainingAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingAssignmentRepository extends MongoRepository<TrainingAssignment, String> {

  List<TrainingAssignment> findByUserId(String userId);

  List<TrainingAssignment> findByTrainingId(String trainingId);

  Optional<TrainingAssignment> findByUserIdAndTrainingId(String userId, String trainingId);

  long countByTrainingId(String trainingId);

  long countByTrainingIdAndStatus(String trainingId, String status);
}
