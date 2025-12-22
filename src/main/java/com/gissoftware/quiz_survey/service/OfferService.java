package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.model.OfferModel;
import com.gissoftware.quiz_survey.repository.OfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final FCMService fcmService;

    public OfferModel createOffer(OfferModel offer) {

        if (offer.getTargetUsers() == null || offer.getTargetUsers().isEmpty()) {
            offer.setTargetUsers(List.of("ALL"));
        }

        OfferModel savedOffer = offerRepository.save(offer);
        fcmService.notifyOfferCreated(savedOffer);
        return savedOffer;
    }

    public OfferModel updateOffer(String id, OfferModel updatedOffer) {

        OfferModel existing = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        if (updatedOffer.getTitle() != null) existing.setTitle(updatedOffer.getTitle());
        if (updatedOffer.getDescription() != null) existing.setDescription(updatedOffer.getDescription());
        if (updatedOffer.getCategory() != null) existing.setCategory(updatedOffer.getCategory());
        if (updatedOffer.getTags() != null) existing.setTags(updatedOffer.getTags());
        if (updatedOffer.getPriority() != null) existing.setPriority(updatedOffer.getPriority());
        if (updatedOffer.getDiscount() != null) existing.setDiscount(updatedOffer.getDiscount());
        if (updatedOffer.getRegion() != null) existing.setRegion(updatedOffer.getRegion());
        if (updatedOffer.getTargetUsers() != null) existing.setTargetUsers(
                updatedOffer.getTargetUsers().isEmpty() ? List.of("ALL") : updatedOffer.getTargetUsers()
        );
        if (updatedOffer.getStatus() != null) existing.setStatus(updatedOffer.getStatus());
        if (updatedOffer.getValidUntil() != null) existing.setValidUntil(updatedOffer.getValidUntil());

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
