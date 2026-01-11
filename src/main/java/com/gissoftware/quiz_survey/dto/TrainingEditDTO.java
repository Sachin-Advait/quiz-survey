package com.gissoftware.quiz_survey.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingEditDTO {

  private String id;

  private String title;
  private String type;
  private String duration;
  private String region;

  // assignment stats (read-only in UI)
  private Integer assignedTo;
  private Integer completionRate;

  // media (Cloudinary now, Bunny later)
  private String mediaUrl;
  private String mediaPublicId;
  private String mediaResourceType;
  private String mediaFormat;

  private Boolean active;
  private Instant uploadDate;

  // ⭐ OPTIONAL (recommended for edit UI)
  private List<String> assignedUserIds;
}
