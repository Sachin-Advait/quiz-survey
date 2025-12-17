package com.gissoftware.quiz_survey.model;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("offers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EnableMongoAuditing
public class OfferModel {

  @Id private String id;

  private String title;
  private String description;

  private String category; // Offer & Plans, Enhancement, etc
  private List<String> tags;

  private String priority; // low | medium | high
  private String discount;

  private String region;
  private List<String> targetUsers;

  private String status; // active | inactive | draft

  private Instant validUntil;

  @CreatedDate private Instant createdAt;
}
