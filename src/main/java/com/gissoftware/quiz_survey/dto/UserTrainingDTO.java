package com.gissoftware.quiz_survey.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserTrainingDTO {

  private String assignmentId;

  private String trainingId;
  private String title;
  private String type;
  private String duration;

  // Cloudinary
  private String cloudinaryUrl;
  private String cloudinaryFormat;
  private String cloudinaryResourceType;

  // User-specific
  private int progress;
  private String status;
  private Instant dueDate;
}
