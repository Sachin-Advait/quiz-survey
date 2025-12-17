package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.model.OfferModel;
import com.gissoftware.quiz_survey.service.OfferService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/offers")
@RequiredArgsConstructor
public class OfferController {

  private final OfferService offerService;

  // 🔐 ADMIN
  @PostMapping
  public ResponseEntity<OfferModel> createOffer(@RequestBody OfferModel offer) {
    return ResponseEntity.ok(offerService.createOffer(offer));
  }

  // 🔐 ADMIN
  @PutMapping("/{id}")
  public ResponseEntity<OfferModel> updateOffer(
      @PathVariable String id, @RequestBody OfferModel offer) {
    return ResponseEntity.ok(offerService.updateOffer(id, offer));
  }

  // 🔐 ADMIN
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteOffer(@PathVariable String id) {
    offerService.deleteOffer(id);
    return ResponseEntity.noContent().build();
  }

  // 👤 USER + ADMIN
  @GetMapping
  public ResponseEntity<List<OfferModel>> getAllOffers() {
    return ResponseEntity.ok(offerService.getAllOffers());
  }

  // 👤 USER (Active only)
  @GetMapping("/active")
  public ResponseEntity<List<OfferModel>> getActiveOffers() {
    return ResponseEntity.ok(offerService.getActiveOffers());
  }

  // 👤 USER + ADMIN
  @GetMapping("/{id}")
  public ResponseEntity<OfferModel> getOffer(@PathVariable String id) {
    return ResponseEntity.ok(offerService.getOfferById(id));
  }

  @GetMapping("/user/{userId}")
  public ResponseEntity<List<OfferModel>> getOffersForUser(@PathVariable String userId) {
    return ResponseEntity.ok(offerService.getOffersForUser(userId));
  }
}
