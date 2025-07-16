package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("announcements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {
    @Id
    private String id;
    private String title;
    private String message;
    private boolean read;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
