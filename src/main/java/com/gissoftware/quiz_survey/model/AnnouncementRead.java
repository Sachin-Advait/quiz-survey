package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.util.HashSet;
import java.util.Set;

@Document("announcement_reads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EnableMongoAuditing
public class AnnouncementRead {
    @Id
    private String userId;

    @CreatedDate
    private Set<String> announcementIds = new HashSet<>();
}

