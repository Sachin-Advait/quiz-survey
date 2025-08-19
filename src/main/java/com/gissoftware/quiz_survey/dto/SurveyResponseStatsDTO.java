package com.gissoftware.quiz_survey.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
public class SurveyResponseStatsDTO {
    private OverallStatsDTO overall;
    private String title;
    private List<BreakdownByRegionDTO> byRegion;

    @Builder
    @Data
    public static class OverallStatsDTO {
        private int totalInvited;
        private int totalResponded;
        private double overallResponseRate;
    }

    @Builder
    @Data
    public static class BreakdownByRegionDTO {
        private String region;
        private int invited;
        private int responded;
        private double responseRate;
        private List<BreakdownByOutletDTO> outlets;
    }

    @Builder
    @Data
    public static class BreakdownByOutletDTO {
        private String outlet;
        private int invited;
        private int responded;
        private double responseRate;
        private List<BreakdownByRoleDTO> position;
    }

    @Builder
    @Data
    public static class BreakdownByRoleDTO {
        private String position;
        private int invited;
        private int responded;
        private double responseRate;
    }
}

