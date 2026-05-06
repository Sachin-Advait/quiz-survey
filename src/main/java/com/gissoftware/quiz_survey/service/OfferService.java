package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.controller.QuizSurveySseController;
import com.gissoftware.quiz_survey.dto.OfferResponseDTO;
import com.gissoftware.quiz_survey.model.OfferModel;
import com.gissoftware.quiz_survey.repository.OfferRepository;
import com.gissoftware.quiz_survey.repository.OfferViewRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfferService {

  private final OfferRepository offerRepository;
  private final FCMService fcmService;
  private final OfferViewRepository offerViewRepository;
  private final QuizSurveySseController quizSurveySseController;

  public OfferModel createOffer(OfferModel offer) {

    OfferValidator.validate(offer);
    if (offer.getTargetUsers() == null || offer.getTargetUsers().isEmpty()) {
      offer.setTargetUsers(List.of("ALL"));
    }

    OfferModel savedOffer = offerRepository.save(offer);
    quizSurveySseController.pushNewOffer(savedOffer);
    fcmService.notifyOfferCreated(savedOffer);
    return savedOffer;
  }

  public OfferModel updateOffer(String id, OfferModel updatedOffer) {

    OfferModel existing =
        offerRepository.findById(id).orElseThrow(() -> new RuntimeException("Offer not found"));

    if (updatedOffer.getType() != null) existing.setType(updatedOffer.getType());
    if (updatedOffer.getTitle() != null) existing.setTitle(updatedOffer.getTitle());
    if (updatedOffer.getDescription() != null)
      existing.setDescription(updatedOffer.getDescription());
    if (updatedOffer.getCategory() != null) existing.setCategory(updatedOffer.getCategory());
    if (updatedOffer.getTags() != null) existing.setTags(updatedOffer.getTags());
    if (updatedOffer.getPriority() != null) existing.setPriority(updatedOffer.getPriority());
    if (updatedOffer.getDiscount() != null) existing.setDiscount(updatedOffer.getDiscount());
    if (updatedOffer.getRegion() != null) existing.setRegion(updatedOffer.getRegion());
    if (updatedOffer.getStatus() != null) existing.setStatus(updatedOffer.getStatus());
    if (updatedOffer.getValidUntil() != null) existing.setValidUntil(updatedOffer.getValidUntil());
    if (updatedOffer.getImageUrl() != null) existing.setImageUrl(updatedOffer.getImageUrl());
    if (updatedOffer.getIsMandatory() != null)
      existing.setIsMandatory(updatedOffer.getIsMandatory());

    if (updatedOffer.getTargetUsers() != null) {
      existing.setTargetUsers(
          updatedOffer.getTargetUsers().isEmpty() ? List.of("ALL") : updatedOffer.getTargetUsers());
    }

    // 🔥 Validate AFTER merge
    OfferValidator.validate(existing);

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

  public List<OfferResponseDTO> getOffersForUser(String userId) {
    List<OfferModel> offers =
        offerRepository.findAll().stream()
            .filter(
                offer ->
                    "active".equalsIgnoreCase(offer.getStatus())
                        && offer.getTargetUsers() != null
                        && (offer.getTargetUsers().contains("ALL")
                            || offer.getTargetUsers().contains(userId)))
            .toList();

    return offers.stream()
        .map(
            offer -> {
              boolean isViewed =
                  offerViewRepository.findByOfferIdAndUserId(offer.getId(), userId).isPresent();

              return OfferResponseDTO.builder()
                  .id(offer.getId())
                  .type(offer.getType())
                  .title(offer.getTitle())
                  .description(offer.getDescription())
                  .category(offer.getCategory())
                  .tags(offer.getTags())
                  .priority(offer.getPriority())
                  .discount(offer.getDiscount())
                  .region(offer.getRegion())
                  .targetUsers(offer.getTargetUsers())
                  .isMandatory(offer.getIsMandatory())
                  .status(offer.getStatus())
                  .validUntil(offer.getValidUntil())
                  .imageUrl(offer.getImageUrl())
                  .createdAt(offer.getCreatedAt())

                  // 🔥 IMPORTANT
                  .isViewed(isViewed)
                  .build();
            })
        .toList();
  }
}
