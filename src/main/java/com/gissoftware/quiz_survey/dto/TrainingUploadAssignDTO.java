package com.gissoftware.quiz_survey.dto;

import com.gissoftware.quiz_survey.model.TrainingMaterial;
import java.time.Instant;
import java.util.List;
import lombok.Data;

@Data
public class TrainingUploadAssignDTO {

  private TrainingMaterial material; // training details
  private List<String> userIds; // learners
  private Instant dueDate; // assignment due date
}
