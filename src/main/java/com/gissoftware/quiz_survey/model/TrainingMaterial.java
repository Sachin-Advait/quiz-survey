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

  // ===== Cloudinary fields =====
  private String cloudinaryPublicId;
  private String cloudinaryUrl;
  private String cloudinaryResourceType;
  private String cloudinaryFormat;

  @CreatedDate private Instant uploadDate;
}
