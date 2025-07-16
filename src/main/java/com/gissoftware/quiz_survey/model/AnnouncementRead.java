package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

@Document("announcement_reads")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementRead {
    @Id
    private String userId;

    @Builder.Default
    private Set<String> announcementIds = new HashSet<>();
}

