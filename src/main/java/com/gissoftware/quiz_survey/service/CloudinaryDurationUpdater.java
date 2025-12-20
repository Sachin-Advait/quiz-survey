package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.model.TrainingMaterial;
import com.gissoftware.quiz_survey.repository.TrainingMaterialRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryDurationUpdater {

  private final TrainingMaterialRepository materialRepo;

  /** Runs every 10 minutes */
  @Scheduled(cron = "0 */10 * * * *")
  public void updateVideoDurations() {

    log.info("⏱️ Running video duration updater using ffprobe");

    List<TrainingMaterial> videos =
        materialRepo.findByCloudinaryResourceTypeAndDuration("video", "N/A");

    if (videos.isEmpty()) {
      log.info("✅ No videos pending duration update");
      return;
    }

    log.info("🔍 Found {} videos with missing duration", videos.size());

    for (TrainingMaterial material : videos) {

      Double seconds = extractDurationFromUrl(material.getCloudinaryUrl());

      if (seconds != null && seconds > 0) {

        String formatted = formatDuration(seconds);
        material.setDuration(formatted);
        materialRepo.save(material);

        log.info("✅ Duration updated → {} = {}", material.getTitle(), formatted);

      } else {
        log.warn("⏳ Could not extract duration for {}", material.getTitle());
      }
    }
  }

  // ---------------- HELPERS ----------------

  private Double extractDurationFromUrl(String videoUrl) {
    try {
      ProcessBuilder pb =
          new ProcessBuilder(
              "ffprobe",
              "-v",
              "error",
              "-show_entries",
              "format=duration",
              "-of",
              "default=noprint_wrappers=1:nokey=1",
              videoUrl);

      pb.redirectErrorStream(true);
      Process process = pb.start();

      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()))) {

        String line = reader.readLine();
        process.waitFor();

        if (line == null || line.isBlank()) return null;
        return Double.parseDouble(line.trim());
      }

    } catch (Exception e) {
      log.error("❌ ffprobe failed for {}", videoUrl, e);
      return null;
    }
  }

  private String formatDuration(double seconds) {
    long mins = (long) (seconds / 60);
    long secs = (long) (seconds % 60);

    return mins == 0 ? secs + " sec" : mins + " min " + secs + " sec";
  }
}
