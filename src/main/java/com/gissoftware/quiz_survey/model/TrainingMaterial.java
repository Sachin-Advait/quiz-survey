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
  private String type;
  private String duration;
  private Integer durationSeconds;

  @org.springframework.data.mongodb.core.index.Indexed private String region;

  private Integer assignedTo;
  private Integer completionRate;
  private Integer views;
  private Instant dueDate;

  private String videoProvider;

  @org.springframework.data.mongodb.core.index.Indexed private String videoPublicId;

  private String videoPlaybackUrl;
  private String videoFormat;

  private String documentUrl;

  @org.springframework.data.mongodb.core.index.Indexed private Boolean active = true;

  private Instant deletedAt;

  @CreatedDate private Instant uploadDate;
}
