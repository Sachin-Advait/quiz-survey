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
  private final UserRepository userRepository;

  public List<OfferViewReportDTO> getOfferViewReport(String offerId) {

    OfferModel offer =
        offerRepository
            .findById(offerId)
            .orElseThrow(() -> new RuntimeException("Offer not found"));

    List<OfferView> views = offerViewRepository.findByOfferId(offerId);

    Map<String, OfferView> viewedMap =
        views.stream().collect(Collectors.toMap(OfferView::getUserId, v -> v));

    List<UserModel> users = userRepository.findAll();

    return users.stream()
        .map(
            user -> {
              OfferView view = viewedMap.get(user.getId());

              boolean viewed = view != null;

              return new OfferViewReportDTO(
                  offer.getId(),
                  offer.getTitle(),
                  user.getId(),
                  user.getUsername(),
                  viewed,
                  viewed ? view.getViewedAt() : null // ✅ FIX
                  );
            })
        .sorted((a, b) -> Boolean.compare(b.isViewed(), a.isViewed()))
        .toList();
  }
}
