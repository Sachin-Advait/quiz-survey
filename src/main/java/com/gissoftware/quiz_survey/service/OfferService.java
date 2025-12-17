package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.model.OfferModel;
import com.gissoftware.quiz_survey.repository.OfferRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferService {

  private final OfferRepository offerRepository;

  public OfferModel createOffer(OfferModel offer) {
    offer.setCreatedAt(Instant.now());

    // ✅ SAFETY: avoid null pointer
    if (offer.getTargetUsers() == null || offer.getTargetUsers().isEmpty()) {
      offer.setTargetUsers(List.of("ALL"));
    }

    return offerRepository.save(offer);
  }

  public OfferModel updateOffer(String id, OfferModel updatedOffer) {
    OfferModel existing =
        offerRepository.findById(id).orElseThrow(() -> new RuntimeException("Offer not found"));

    existing.setTitle(updatedOffer.getTitle());
    existing.setDescription(updatedOffer.getDescription());
    existing.setCategory(updatedOffer.getCategory());
    existing.setTags(updatedOffer.getTags());
    existing.setPriority(updatedOffer.getPriority());
    existing.setDiscount(updatedOffer.getDiscount());
    existing.setRegion(updatedOffer.getRegion());

    // 🔥 FIXED HERE
    existing.setTargetUsers(
        updatedOffer.getTargetUsers() == null || updatedOffer.getTargetUsers().isEmpty()
            ? List.of("ALL")
            : updatedOffer.getTargetUsers());

    existing.setStatus(updatedOffer.getStatus());
    existing.setValidUntil(updatedOffer.getValidUntil());

    return offerRepository.save(existing);
  }

  public void deleteOffer(String id) {
    if (!offerRepository.existsById(id)) {
      throw new RuntimeException("Offer not found");
    }
    offerRepository.deleteById(id);
  }

  public List<OfferModel> getAllOffers() {
    return offerRepository.findAll();
  }

  public List<OfferModel> getActiveOffers() {
    return offerRepository.findByStatus("active");
  }

  public OfferModel getOfferById(String id) {
    return offerRepository.findById(id).orElseThrow(() -> new RuntimeException("Offer not found"));
  }

  public List<OfferModel> getOffersForUser(String userId) {

    return offerRepository.findAll().stream()
        .filter(
            offer ->
                "active".equalsIgnoreCase(offer.getStatus())
                    && offer.getTargetUsers() != null
                    && (offer.getTargetUsers().contains("ALL")
                        || offer.getTargetUsers().contains(userId)))
        .toList();
  }
}
