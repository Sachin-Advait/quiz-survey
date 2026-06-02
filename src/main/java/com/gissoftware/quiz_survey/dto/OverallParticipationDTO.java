package com.gissoftware.quiz_survey.dto;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OverallParticipationDTO {

  private String quizSurveyId;
  private String title;
  private String type;
  private String region;
  private String outlet;

  private String userId;
  private String staffId;
  private String username;

  private boolean participated;

  private Integer score;
  private Integer maxScore;
  private Double percentage;
  private Integer marks;

  private String result;
  private Boolean completion;
  private Instant quizOpenTime;
  private Instant agentOpenTime;
  private Instant agentSubmissionTime;

  // Dynamic columns
  private Map<String, String> questionAnswers;
  private Map<String, String> correctAnswers;
  private Map<String, Integer> questionMarks;
  private Map<String, String> questionArabicTitles;
}
