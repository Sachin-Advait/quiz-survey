package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.model.OfferModel;
import com.gissoftware.quiz_survey.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    // 🔐 ADMIN
    @PostMapping
    public ResponseEntity<ApiResponseDTO<OfferModel>> createOffer(@RequestBody OfferModel offer) {
        OfferModel created = offerService.createOffer(offer);
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Offer created successfully", created)
        );
    }

    // 🔐 ADMIN
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<OfferModel>> updateOffer(@PathVariable String id, @RequestBody OfferModel offer) {
        OfferModel updated = offerService.updateOffer(id, offer);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Offer updated successfully", updated));
    }

    // 🔐 ADMIN
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteOffer(@PathVariable String id) {
        offerService.deleteOffer(id);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Offer deleted successfully", null));
    }

    // 👤 USER + ADMIN
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<OfferModel>>> getAllOffers() {
        List<OfferModel> offers = offerService.getAllOffers();
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "All offers fetched successfully", offers)
        );
    }

    // 👤 USER (Active only)
    @GetMapping("/active")
    public ResponseEntity<ApiResponseDTO<List<OfferModel>>> getActiveOffers() {
        List<OfferModel> offers = offerService.getActiveOffers();
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Active offers fetched successfully", offers)
        );
    }

    // 👤 USER + ADMIN
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<OfferModel>> getOffer(@PathVariable String id) {
        OfferModel offer = offerService.getOfferById(id);
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Offer fetched successfully", offer)
        );
    }

    // 👤 USER
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponseDTO<List<OfferModel>>> getOffersForUser(@PathVariable String userId) {
        List<OfferModel> offers = offerService.getOffersForUser(userId);
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "User offers fetched successfully", offers)
        );
    }
}
