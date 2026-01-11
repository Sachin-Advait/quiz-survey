package com.gissoftware.quiz_survey.controller;

import com.cloudinary.Cloudinary;
import com.gissoftware.quiz_survey.dto.SyncResult;
import com.gissoftware.quiz_survey.service.CloudinarySyncService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cloudinary")
@RequiredArgsConstructor
public class CloudinarySyncController {

  private final CloudinarySyncService syncService;
  private final Cloudinary cloudinary;

  @PostMapping("/sync")
  public ResponseEntity<?> sync(@RequestBody Map<String, Object> payload) throws Exception {

    List<String> resourceTypes =
        (List<String>) payload.getOrDefault("resourceTypes", List.of("video", "raw"));

    String folder = (String) payload.get("folder");
    boolean dryRun = Boolean.TRUE.equals(payload.get("dryRun"));

    SyncResult result = syncService.sync(resourceTypes, folder, dryRun);

    return ResponseEntity.ok(
        Map.of(
            "success", true,
            "scanned", result.getScanned(),
            "inserted", result.getInserted(),
            "skipped", result.getSkipped(),
            "message", "Cloudinary sync completed"));
  }

  @GetMapping("/signature")
  public Map<String, String> getSignature() {
    long timestamp = System.currentTimeMillis() / 1000;
    String uploadPreset = "training_videos";

    String signature =
        cloudinary.apiSignRequest(
            Map.of("timestamp", timestamp, "upload_preset", uploadPreset),
            cloudinary.config.apiSecret);

    return Map.of(
        "timestamp",
        String.valueOf(timestamp),
        "signature",
        signature,
        "apiKey",
        cloudinary.config.apiKey,
        "cloudName",
        cloudinary.config.cloudName,
        "uploadPreset",
        uploadPreset);
  }
}
