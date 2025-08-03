package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SurveyActivityStatsDTO {

    private List<RegionActivityDTO> mostActiveRegions;
    private List<RegionActivityDTO> leastActiveRegions;
    private List<OutletActivityDTO> mostActiveOutlets;
    private List<OutletActivityDTO> leastActiveOutlets;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RegionActivityDTO {
        private String region;
        private int responses;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OutletActivityDTO {
        private String outlet;
        private int responses;
    }
}

