package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.OfferViewReportDTO;
import com.gissoftware.quiz_survey.model.OfferModel;
import com.gissoftware.quiz_survey.model.OfferView;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.OfferRepository;
import com.gissoftware.quiz_survey.repository.OfferViewRepository;
import com.gissoftware.quiz_survey.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferReportService {

  private final OfferRepository offerRepository;
  private final OfferViewRepository offerViewRepository;
  private final UserRepository userRepository; // or however you store users

  public List<OfferViewReportDTO> getOfferViewReport(String offerId) {

    OfferModel offer =
        offerRepository
            .findById(offerId)
            .orElseThrow(() -> new RuntimeException("Offer not found"));

    List<OfferView> views = offerViewRepository.findByOfferId(offerId);

    Map<String, OfferView> viewedMap =
        views.stream().collect(Collectors.toMap(OfferView::getUserId, v -> v));

    // All users eligible for this offer
    List<UserModel> users = userRepository.findAll();
    // OR filter by region / targetUsers if needed

    return users.stream()
        .map(
            user ->
                new OfferViewReportDTO(
                    offer.getId(),
                    offer.getTitle(),
                    user.getId(),
                    user.getUsername(),
                    viewedMap.containsKey(user.getId())))
        // 🔥 sort: viewed=true first, viewed=false last
        .sorted((a, b) -> Boolean.compare(b.isViewed(), a.isViewed()))
        .toList();
  }
}
