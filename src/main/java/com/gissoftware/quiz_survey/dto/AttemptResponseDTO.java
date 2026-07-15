package com.gissoftware.quiz_survey.dto;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttemptResponseDTO {
  private String attemptId;
  private String status; // IN_PROGRESS, SUBMITTED, AUTO_SUBMITTED
  private String message;

  // For IN_PROGRESS
  private Instant startTime;
  private Instant endTime;
  private Integer durationMinutes;
  private Long remainingSeconds;
  private Map<String, Object> answers;
  private Integer currentPageNo;
  private Integer attemptNumber;

  // For completed
  private Instant submittedAt;
}
