package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
}