package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SurveyActivityStatsDTO {

    private RegionActivityDTO mostActiveRegions;
    private RegionActivityDTO leastActiveRegions;
    private OutletActivityDTO mostActiveOutlets;
    private OutletActivityDTO leastActiveOutlets;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class RegionActivityDTO {
        private String region;
        private int totalUsers;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OutletActivityDTO {
        private String outlet;
        private int totalUsers;
    }
}
