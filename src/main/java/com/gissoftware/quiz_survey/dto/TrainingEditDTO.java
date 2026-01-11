package com.gissoftware.quiz_survey.dto;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingEditDTO {

    private String id;
    private String title;
    private String type;
    private String duration;
    private String region;

    private Integer assignedTo;
    private Integer completionRate;

    // ✅ VIDEO (GENERIC)
    private String videoProvider;
    private String videoPublicId;
    private String videoPlaybackUrl;
    private String videoFormat;

    private Boolean active;
    private Instant uploadDate;

    private List<String> assignedUserIds;
}
