package com.gissoftware.quiz_survey.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowScoringUserDTO {
    private String userId;
    private String username;
    private String staffId;
    private Double avgPercentage;
    private List<String> attemptedQuizzes;
    private Long attemptCount;
    private String region;
    private String outlet;

}