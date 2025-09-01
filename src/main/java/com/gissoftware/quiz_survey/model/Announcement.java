package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document("announcements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EnableMongoAuditing
public class Announcement {
    @Id
    private String id;
    private String title;
    private String message;
    private List<String> targetUser;

    @CreatedDate
    private Instant createdAt;
}
