package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.*;
import com.gissoftware.quiz_survey.model.TrainingAssignment;
import com.gissoftware.quiz_survey.model.TrainingMaterial;
import com.gissoftware.quiz_survey.service.TrainingService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/training")
@RequiredArgsConstructor
public class TrainingController {

  private final TrainingService trainingService;

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

  @GetMapping("/bunny/tus-signature")
  public ResponseEntity<Map<String, String>> getTusSignature(@RequestParam String videoId) {
    long expires = Instant.now().getEpochSecond() + 15 * 60; // 15 min

    String raw = libraryId + bunnyApiKey + expires + videoId;
    String signature = DigestUtils.sha256Hex(raw);

    return ResponseEntity.ok(
        Map.of(
            "signature", signature,
            "expires", String.valueOf(expires),
            "libraryId", libraryId,
            "videoId", videoId));
  }

  @PostMapping("/bunny/upload-document")
  public ResponseEntity<Map<String, String>> uploadDocument(
      @RequestParam("file") MultipartFile file) throws Exception {

    // 1️⃣ Validate file type
    List<String> allowed =
        List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation");

    if (!allowed.contains(file.getContentType())) {
      throw new IllegalArgumentException("Unsupported document type");
    }

    // 3️⃣ Generate filename
    String fileName =
        System.currentTimeMillis() + "_" + file.getOriginalFilename().replaceAll("\\s+", "_");

    String uploadUrl = "https://sg.storage.bunnycdn.com/" + bunnyStorageZone + "/" + fileName;

    // 4️⃣ Upload to Bunny Storage
    HttpHeaders headers = new HttpHeaders();
    headers.set("AccessKey", bunnyStorageApiKey);
    headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

    HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

    RestTemplate restTemplate = new RestTemplate();
    restTemplate.put(uploadUrl, entity);

    // 5️⃣ Public CDN URL
    return ResponseEntity.ok(
        Map.of(
            "documentUrl",
            bunnyStorageCdnUrl + "/" + fileName,
            "fileName",
            file.getOriginalFilename(),
            "type",
            "document"));
  }

  @GetMapping("/bunny/upload-signature")
  public ResponseEntity<Map<String, String>> getUploadSignature(
      @RequestParam String videoId, @RequestParam long fileSize) {

    long expires = Instant.now().getEpochSecond() + 600;

    String raw = libraryId + videoId + expires + fileSize + bunnyApiKey;
    String signature = DigestUtils.sha256Hex(raw);

    return ResponseEntity.ok(
        Map.of(
            "videoId",
            videoId,
            "libraryId",
            libraryId,
            "expires",
            String.valueOf(expires),
            "signature",
            signature,
            "uploadUrl",
            "https://video.bunnycdn.com/library/" + libraryId + "/videos/" + videoId));
  }

  @PostMapping("/bunny/create-video")
  public ResponseEntity<Map<String, String>> createVideo(@RequestParam String title) {

    RestTemplate rest = new RestTemplate();

    HttpHeaders headers = new HttpHeaders();
    headers.set("AccessKey", bunnyApiKey);
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, String>> req = new HttpEntity<>(Map.of("title", title), headers);

    String url = "https://video.bunnycdn.com/library/" + libraryId + "/videos";
    ResponseEntity<Map> res = rest.postForEntity(url, req, Map.class);

    String videoId = (String) res.getBody().get("guid");

    return ResponseEntity.ok(
        Map.of(
            "videoId",
            videoId,
            "uploadUrl",
            "https://video.bunnycdn.com/library/" + libraryId + "/videos/" + videoId));
  }

  @GetMapping("/bunny/upload-token")
  public ResponseEntity<Map<String, String>> getUploadToken() {
    return ResponseEntity.ok(
        Map.of(
            "accessKey", bunnyApiKey // you can rotate later if needed
            ));
  }

  // ================= ADMIN =================
  //  @PostMapping
  //  public ResponseEntity<ApiResponseDTO<TrainingMaterial>> uploadTraining(
  //      @RequestBody TrainingUploadAssignDTO request) {
  //
  //    TrainingMaterial savedMaterial = trainingService.uploadAndAssign(request);
  //
  //    return ResponseEntity.ok(
  //        new ApiResponseDTO<>(true, "Training uploaded and assigned successfully",
  // savedMaterial));
  //  }
  @PostMapping
  public ResponseEntity<ApiResponseDTO<Void>> uploadTraining(
      @RequestBody TrainingUploadAssignDTO request) {

    trainingService.uploadAndAssignAsync(request);

    return ResponseEntity.accepted()
        .body(new ApiResponseDTO<>(true, "Training uploaded and assignment started", null));
  }

  @GetMapping
  public ResponseEntity<ApiResponseDTO<List<TrainingMaterial>>> getAllTrainings() {
    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true, "All trainings fetched successfully", trainingService.getAllMaterials()));
  }

  @GetMapping("/region/{region}")
  public ResponseEntity<ApiResponseDTO<List<TrainingMaterial>>> getByRegion(
      @PathVariable String region) {
    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true, "Trainings fetched for region", trainingService.getMaterialsByRegion(region)));
  }

  @PostMapping("/assign")
  public ResponseEntity<ApiResponseDTO<Void>> assignTraining(
      @RequestBody Map<String, Object> payload) {
    String trainingId = (String) payload.get("trainingId");
    List<String> userIds = (List<String>) payload.get("userIds");
    Instant dueDate = Instant.parse((String) payload.get("dueDate"));

    trainingService.assignTraining(trainingId, userIds, dueDate);

    return ResponseEntity.ok(new ApiResponseDTO<>(true, "Training assigned successfully", null));
  }

  // ================= USER =================
  @GetMapping("/user/{userId}")
  public ResponseEntity<ApiResponseDTO<List<TrainingAssignment>>> getUserTrainings(
      @PathVariable String userId) {
    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true, "User trainings fetched successfully", trainingService.getUserTrainings(userId)));
  }

  @GetMapping("/user/{userId}/details")
  public ResponseEntity<ApiResponseDTO<List<UserTrainingDTO>>> getUserTrainingDetails(
      @PathVariable String userId) {
    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true,
            "User training details fetched successfully",
            trainingService.getUserTrainingDetails(userId)));
  }

  @PostMapping("/progress")
  public ResponseEntity<ApiResponseDTO<TrainingAssignment>> updateProgress(
      @RequestBody Map<String, Object> payload) {
    String userId = (String) payload.get("userId");
    String trainingId = (String) payload.get("trainingId");
    Integer progress = (Integer) payload.get("progress");

    TrainingAssignment updated = trainingService.updateProgress(userId, trainingId, progress);
    return ResponseEntity.ok(
        new ApiResponseDTO<>(true, "Training progress updated successfully", updated));
  }

  @GetMapping("/engagement")
  public ResponseEntity<ApiResponseDTO<List<TrainingEngagementDTO>>> getEngagement(
      @RequestParam(required = false) String trainingId) {
    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true, "Engagement fetched successfully", trainingService.getEngagement(trainingId)));
  }

  @GetMapping("/{trainingId}/engagement")
  public ResponseEntity<ApiResponseDTO<List<TrainingEngagementDTO>>> getEngagementByTrainingId(
      @PathVariable String trainingId) {

    List<TrainingEngagementDTO> engagement = trainingService.getEngagementByTrainingId(trainingId);

    return ResponseEntity.ok(
        new ApiResponseDTO<>(true, "Engagement fetched successfully", engagement));
  }

  // ================= ADMIN =================

  @PutMapping("/{trainingId}")
  public ResponseEntity<ApiResponseDTO<TrainingMaterial>> updateTraining(
      @PathVariable String trainingId, @RequestBody TrainingUploadAssignDTO request) {

    TrainingMaterial updated = trainingService.updateTraining(trainingId, request);

    return ResponseEntity.ok(new ApiResponseDTO<>(true, "Training updated successfully", updated));
  }

  @DeleteMapping("/{trainingId}")
  public ResponseEntity<ApiResponseDTO<Void>> deleteTraining(@PathVariable String trainingId) {

    trainingService.deleteTraining(trainingId);

    return ResponseEntity.ok(new ApiResponseDTO<>(true, "Training deleted successfully", null));
  }

  @GetMapping("/{trainingId}")
  public ResponseEntity<ApiResponseDTO<TrainingEditDTO>> getTrainingById(
      @PathVariable String trainingId) {

    return ResponseEntity.ok(
        new ApiResponseDTO<>(
            true, "Training fetched successfully", trainingService.getTrainingById(trainingId)));
  }

  @GetMapping("/bunny/video-status/{videoId}")
  public ResponseEntity<Map<String, Object>> getBunnyVideoStatus(@PathVariable String videoId) {

    String url = "https://video.bunnycdn.com/library/" + libraryId + "/videos/" + videoId;

    HttpHeaders headers = new HttpHeaders();
    headers.set("AccessKey", bunnyApiKey);

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    RestTemplate restTemplate = new RestTemplate();

    ResponseEntity<Map> response =
        restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, Map.class);

    Map body = response.getBody();

    return ResponseEntity.ok(
        Map.of(
            "status", body.get("status"),
            "encodeProgress", body.getOrDefault("encodeProgress", 0)));
  }
}
