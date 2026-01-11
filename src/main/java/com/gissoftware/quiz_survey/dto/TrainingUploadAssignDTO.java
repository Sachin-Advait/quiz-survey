package com.gissoftware.quiz_survey.dto;

import com.gissoftware.quiz_survey.model.TrainingMaterial;
import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class TrainingUploadAssignDTO {

  private TrainingMaterial material;

  private List<String> userIds;
  private Instant dueDate;

  // VIDEO (generic)
  private String videoProvider; // bunny | cloudinary | s3
  private String videoPublicId; // videoId / objectKey
  private String videoPlaybackUrl; // optional
  private String videoFormat; // hls | mp4
}
