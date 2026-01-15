package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.OfferView;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OfferViewRepository extends MongoRepository<OfferView, String> {

    Optional<OfferView> findByOfferIdAndUserId(String offerId, String userId);

    List<OfferView> findByOfferId(String offerId);
}
