package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.OfferModel;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OfferRepository extends MongoRepository<OfferModel, String> {

  List<OfferModel> findByStatus(String status);

  List<OfferModel> findByTargetUsersIn(List<String> userIds);

  List<OfferModel> findByCategory(String category);

  List<OfferModel> findByTagsIn(List<String> tags);

  List<OfferModel> findByRegion(String region);

  List<OfferModel> findByPriority(String priority);
}
