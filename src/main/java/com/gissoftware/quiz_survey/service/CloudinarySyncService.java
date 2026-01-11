// package com.gissoftware.quiz_survey.service;
//
// import com.cloudinary.Cloudinary;
// import com.gissoftware.quiz_survey.dto.SyncResult;
// import com.gissoftware.quiz_survey.model.TrainingMaterial;
// import com.gissoftware.quiz_survey.repository.TrainingMaterialRepository;
// import java.time.Instant;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.stereotype.Service;
//
// @Service
// @RequiredArgsConstructor
// @Slf4j
// public class CloudinarySyncService {
//
//  private final Cloudinary cloudinary;
//  private final TrainingMaterialRepository materialRepo;
//
//  @SuppressWarnings("unchecked")
//  public SyncResult sync(List<String> resourceTypes, String folder, boolean dryRun)
//      throws Exception {
//
//    int scanned = 0;
//    int inserted = 0;
//    int skipped = 0;
//
//    log.info("===== CLOUDINARY SYNC START =====");
//    log.info("Cloud name     : {}", cloudinary.config.cloudName);
//    log.info("Resource types : {}", resourceTypes);
//    log.info("Folder prefix  : {}", folder);
//    log.info("Dry run        : {}", dryRun);
//
//    for (String resourceType : resourceTypes) {
//
//      log.info("---- Fetching resource_type={} ----", resourceType);
//
//      Map<String, Object> options = new HashMap<>();
//      options.put("type", "upload");
//      options.put("resource_type", resourceType);
//      options.put("max_results", 500);
//
//      if (folder != null && !folder.isBlank()) {
//        options.put("prefix", folder);
//      }
//
//      Map<String, Object> result = cloudinary.api().resources(options);
//      List<Map<String, Object>> resources = (List<Map<String, Object>>) result.get("resources");
//
//      log.info(
//          "Cloudinary returned {} assets for resource_type={}",
//          resources == null ? 0 : resources.size(),
//          resourceType);
//
//      if (resources == null || resources.isEmpty()) continue;
//
//      for (Map<String, Object> r : resources) {
//
//        scanned++;
//
//        String publicId = (String) r.get("public_id");
//        String secureUrl = (String) r.get("secure_url");
//        String format = (String) r.get("format");
//        Double duration = (Double) r.get("duration");
//
//        // ------------------------------------
//        // 🚫 Ignore Cloudinary demo/sample assets
//        // ------------------------------------
//        if (isDemoAsset(publicId)) {
//          log.info("IGNORED (demo asset) → {}", publicId);
//          continue;
//        }
//
//        // ------------------------------------
//        // 🎥 FIX: Fetch real duration for videos
//        // ------------------------------------
//        if ("video".equals(resourceType) && duration == null) {
//          try {
//            Map<String, Object> videoDetails =
//                cloudinary
//                    .api()
//                    .resource(
//                        publicId,
//                        Map.of(
//                            "resource_type", "video",
//                            "type", "upload"));
//
//            Object d = videoDetails.get("duration");
//            if (d instanceof Number) {
//              duration = ((Number) d).doubleValue();
//            }
//          } catch (Exception ex) {
//            log.warn("Could not fetch duration for video {}", publicId);
//          }
//        }
//
//        log.info(
//            "ASSET → public_id={}, resource_type={}, format={}, duration={}",
//            publicId,
//            resourceType,
//            format,
//            duration);
//
//        // ------------------------------------
//        // 🔁 Skip already synced assets
//        // ------------------------------------
//        if (materialRepo.existsByCloudinaryPublicIdAndActiveTrue(publicId)) {
//          skipped++;
//          log.info("SKIPPED (already exists) → {}", publicId);
//          continue;
//        }
//
//        // ------------------------------------
//        // 🧪 Dry run
//        // ------------------------------------
//        if (dryRun) {
//          inserted++;
//          log.info("DRY-RUN INSERT → {}", publicId);
//          continue;
//        }
//
//        // ------------------------------------
//        // 💾 Save TrainingMaterial
//        // ------------------------------------
//        TrainingMaterial material =
//            TrainingMaterial.builder()
//                .title(extractTitle(publicId))
//                .type(resolveMaterialType(resourceType))
//                .duration("video".equals(resourceType) ? formatDuration(duration) : "N/A")
//                .region("all")
//                .assignedTo(0)
//                .completionRate(0)
//                .views(0)
//                .cloudinaryPublicId(publicId)
//                .cloudinaryUrl(secureUrl)
//                .cloudinaryResourceType(resourceType)
//                .cloudinaryFormat(format)
//                .uploadDate(Instant.now())
//                .build();
//
//        materialRepo.save(material);
//        inserted++;
//
//        log.info("INSERTED → {}", publicId);
//      }
//    }
//
//    log.info("===== CLOUDINARY SYNC COMPLETE =====");
//    log.info("Scanned={}, Inserted={}, Skipped={}", scanned, inserted, skipped);
//
//    return new SyncResult(scanned, inserted, skipped);
//  }
//
//  // ======================================================
//  // Helper methods
//  // ======================================================
//
//  private boolean isDemoAsset(String publicId) {
//    return publicId == null
//        || publicId.startsWith("samples/")
//        || publicId.startsWith("cld-sample")
//        || publicId.equals("sample")
//        || publicId.startsWith("main-sample");
//  }
//
//  private String resolveMaterialType(String resourceType) {
//    if ("video".equals(resourceType)) return "video";
//    if ("image".equals(resourceType)) return "image";
//    return "document"; // raw / pdf
//  }
//
//  private String extractTitle(String publicId) {
//    String name = publicId.substring(publicId.lastIndexOf("/") + 1);
//    name = name.replaceAll("_[a-z0-9]{5,6}$", ""); // remove Cloudinary suffix
//    return name.replace("_", " ");
//  }
//
//  private String formatDuration(Double seconds) {
//    if (seconds == null) return "N/A";
//    long mins = Math.round(seconds / 60.0);
//    return mins + " min";
//  }
// }
