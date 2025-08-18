package com.gissoftware.quiz_survey.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class QuizResponseByRegionDTO {
    private String title;
    private Map<String, Integer> byRegion;
    private Map<String, Integer> byRole;
}
