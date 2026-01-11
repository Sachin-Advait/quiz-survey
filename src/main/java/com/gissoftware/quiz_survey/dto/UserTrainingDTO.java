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

  // ✅ VIDEO (GENERIC)
  private String videoProvider;
  private String videoPublicId;
  private String videoPlaybackUrl;
  private String videoFormat;

  private int progress;
  private String status;
  private Instant dueDate;
}
