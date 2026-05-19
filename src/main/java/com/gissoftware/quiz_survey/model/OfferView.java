package com.gissoftware.quiz_survey.model;

import java.time.Instant;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("offer_views")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
  @CompoundIndex(name = "offer_user_unique", def = "{'offerId': 1, 'userId': 1}", unique = true)
})
public class OfferView {

  @Id private String id;

  @Indexed private String offerId;

  private String userId;
  private String userName;

  private Instant viewedAt;
  private String platform; // WEB or MOBILE
  private String client; // Chrome, Edge, Android, IOS etc.
}
