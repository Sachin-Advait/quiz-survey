package com.gissoftware.quiz_survey.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("training_materials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingMaterial {

  @Id private String id;

  private String title;

  private String type; // video, document, interactive
  private String duration;

  private String region;

  private Integer assignedTo;
  private Integer completionRate;
  private Integer views;

  /* =========================
  VIDEO (ANY PROVIDER)
  ========================= */
  private String videoProvider; // cloudinary | bunny | s3 | vimeo
  private String videoPublicId; // provider video id
  private String videoPlaybackUrl; // signed / iframe url (optional)
  private String videoFormat; // mp4, hls, dash (optional)

  /* =========================
  DOCUMENTS (ANY CDN)
  ========================= */
  private String documentUrl;

  private Boolean active = true;
  private Instant deletedAt;

  @CreatedDate private Instant uploadDate;
}
