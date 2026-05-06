package com.gissoftware.quiz_survey.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OfferViewReportDTO {

  private String offerId;
  private String offerTitle;

  private String userId;
  private String userName;
  private String staffId;

  private boolean viewed;
  private Instant viewedAt;
}
