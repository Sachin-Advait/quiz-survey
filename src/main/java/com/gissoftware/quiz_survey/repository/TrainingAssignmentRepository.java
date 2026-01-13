package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.dto.TrainingEngagementDTO;
import com.gissoftware.quiz_survey.model.TrainingAssignment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingAssignmentRepository extends MongoRepository<TrainingAssignment, String> {

  List<TrainingAssignment> findByUserId(String userId);

  List<TrainingAssignment> findByTrainingId(String trainingId);

  Optional<TrainingAssignment> findByUserIdAndTrainingId(String userId, String trainingId);

  long countByTrainingId(String trainingId);

  long countByTrainingIdAndStatus(String trainingId, String status);

  void deleteByTrainingId(String trainingId);

  @Aggregation(
      pipeline = {
        "{ $match: { $or: [ { $expr: { $eq: [?0, null] } }, { trainingId: ?0 } ] } }",

        // ✅ convert userId → ObjectId
        "{ $addFields: { userObjectId: { $toObjectId: '$userId' } } }",

        // ✅ convert trainingId → ObjectId
        "{ $addFields: { trainingObjectId: { $toObjectId: '$trainingId' } } }",

        // join users
        "{ $lookup: { from: 'users', localField: 'userObjectId', foreignField: '_id', as: 'user' } }",
        "{ $unwind: '$user' }",

        // join training materials
        "{ $lookup: { from: 'training_materials', localField: 'trainingObjectId', foreignField: '_id', as: 'training' } }",
        "{ $unwind: '$training' }",

        // projection
        "{ $project: { "
            + "'userId': '$user._id', "
            + "'learner': '$user.username', "
            + "'trainingId': '$training._id', "
            + "'video': '$training.title', "
            + "'progress': '$progress', "
            + "'status': '$status' "
            + "} }"
      })
  List<TrainingEngagementDTO> fetchEngagement(String trainingId);
}
