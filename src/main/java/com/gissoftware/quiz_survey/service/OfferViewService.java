package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.model.OfferView;
import com.gissoftware.quiz_survey.repository.OfferViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferViewService {

  private final OfferViewRepository offerViewRepository;
  private final UserService userService; // 👈 inject this

  public void markOfferViewed(String offerId, String userId) {

    boolean alreadyViewed = offerViewRepository.findByOfferIdAndUserId(offerId, userId).isPresent();

    if (alreadyViewed) {
      return; // idempotent
    }

    // 🔐 Resolve userName from backend
    String userName = userService.getUserNameById(userId);

    OfferView view = OfferView.builder().offerId(offerId).userId(userId).userName(userName).build();

    offerViewRepository.save(view);
  }
}
