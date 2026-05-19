package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.dto.OfferResponseDTO;
import com.gissoftware.quiz_survey.model.OfferModel;
import com.gissoftware.quiz_survey.service.OfferService;
import com.gissoftware.quiz_survey.service.OfferViewService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/offers")
@RequiredArgsConstructor
public class OfferController {

  private final OfferService offerService;
  private final OfferViewService offerViewService;
  private final HttpServletRequest httpRequest;

  @Value("${bunny.storage.api-key}")
  private String bunnyStorageApiKey;

  @Value("${bunny.storage.cdn-url}")
  private String bunnyStorageCdnUrl;

  @Value("${bunny.library-id}")
  private String libraryId;

  @Value("${bunny.storage.zone}")
  private String bunnyStorageZone;

  @Value("${bunny.api-key}")
  private String bunnyApiKey;

  @PostMapping("/bunny/upload-image")
  public ResponseEntity<Map<String, String>> uploadOfferImage(
      @RequestParam("file") MultipartFile file) throws Exception {

    // 1️⃣ Validate
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("Image file is required");
    }

    List<String> allowed = List.of("image/jpeg", "image/png", "image/webp", "image/jpg");

    if (!allowed.contains(file.getContentType())) {
      throw new IllegalArgumentException("Only image files are allowed");
    }

    // 2️⃣ Filename
    String original = file.getOriginalFilename();
    if (original == null || !original.contains(".")) {
      throw new IllegalArgumentException("Invalid image file");
    }
    String extension = original.substring(original.lastIndexOf("."));

    String fileName =
        "offers/" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID() + extension;

    // 3️⃣ Bunny upload URL
    String uploadUrl = "https://sg.storage.bunnycdn.com/" + bunnyStorageZone + "/" + fileName;

    HttpHeaders headers = new HttpHeaders();
    headers.set("AccessKey", bunnyStorageApiKey);
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

    HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

    new RestTemplate().put(uploadUrl, entity);

    // 4️⃣ Return CDN URL
    return ResponseEntity.ok(
        Map.of("imageUrl", bunnyStorageCdnUrl + "/" + fileName, "type", "image"));
  }

  // 🔐 ADMIN
  @PostMapping
  public ResponseEntity<ApiResponseDTO<OfferModel>> createOffer(@RequestBody OfferModel offer) {
    OfferModel created = offerService.createOffer(offer);
    return ResponseEntity.ok(new ApiResponseDTO<>(true, "Offer created successfully", created));
  }

  // 🔐 ADMIN
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponseDTO<OfferModel>> updateOffer(
      @PathVariable String id, @RequestBody OfferModel offer) {
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
    return ResponseEntity.ok(new ApiResponseDTO<>(true, "All offers fetched successfully", offers));
  }

  // 👤 USER (Active only)
  @GetMapping("/active")
  public ResponseEntity<ApiResponseDTO<List<OfferModel>>> getActiveOffers() {
    List<OfferModel> offers = offerService.getActiveOffers();
    return ResponseEntity.ok(
        new ApiResponseDTO<>(true, "Active offers fetched successfully", offers));
  }

  // 👤 USER + ADMIN
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponseDTO<OfferModel>> getOffer(@PathVariable String id) {
    OfferModel offer = offerService.getOfferById(id);
    return ResponseEntity.ok(new ApiResponseDTO<>(true, "Offer fetched successfully", offer));
  }

  // 👤 USER
  @GetMapping("/user/{userId}")
  public ResponseEntity<ApiResponseDTO<List<OfferResponseDTO>>> getOffersForUser(
      @PathVariable String userId) {

    List<OfferResponseDTO> offers = offerService.getOffersForUser(userId);

    return ResponseEntity.ok(
        new ApiResponseDTO<>(true, "User offers fetched successfully", offers));
  }

  @PutMapping("/{offerId}/view")
  public ResponseEntity<ApiResponseDTO<Void>> markOfferAsViewed(
      @PathVariable String offerId, @RequestParam String userId) {
    String userAgent = httpRequest.getHeader("User-Agent");

    offerViewService.markOfferViewed(offerId, userId, userAgent);

    return ResponseEntity.ok(new ApiResponseDTO<>(true, "Offer marked as viewed", null));
  }
}
