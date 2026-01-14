package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.NotificationModel;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationRepository extends MongoRepository<NotificationModel, String> {

  List<NotificationModel> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId);

  long countByUserIdAndReadFalseAndDeletedFalse(String userId);
}
