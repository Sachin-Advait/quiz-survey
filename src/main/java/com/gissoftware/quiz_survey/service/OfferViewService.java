package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.Utils.DeviceDetectorUtil;
import com.gissoftware.quiz_survey.model.OfferView;
import com.gissoftware.quiz_survey.repository.OfferViewRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferViewService {

  private final OfferViewRepository offerViewRepository;
  private final UserService userService;

  public void markOfferViewed(String offerId, String userId, String userAgent) {

    OfferView view = offerViewRepository.findByOfferIdAndUserId(offerId, userId).orElse(null);

    if (view == null) {
      String userName = userService.getUserNameById(userId);

      view = OfferView.builder().offerId(offerId).userId(userId).userName(userName).build();
    }

    // ✅ ALWAYS update view date
    view.setViewedAt(Instant.now());
    String platform = DeviceDetectorUtil.detectPlatform(userAgent);
    String client = DeviceDetectorUtil.detectClient(userAgent);

    view.setPlatform(platform);
    view.setClient(client);

    offerViewRepository.save(view);
  }
}
