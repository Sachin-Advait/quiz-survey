package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class AnnouncementWithReadStatus {
    private String id;
    private String title;
    private String message;
    private Instant createdAt;
    private boolean read;
}
