package com.gissoftware.quiz_survey.dto;

import com.gissoftware.quiz_survey.model.OfferType;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OfferResponseDTO {
  private String id;

  private OfferType type;

  private String title;
  private String description;

  private String category; // Offer & Plans, Enhancement, etc
  private List<String> tags;

  private String priority; // low | medium | high
  private String discount;

  private String region;
  private List<String> targetUsers;
  private Boolean isMandatory;

  private String status; // active | inactive | draft

  private Instant validUntil;

  private String imageUrl;

  private Instant createdAt;
  private Boolean isViewed;
}
