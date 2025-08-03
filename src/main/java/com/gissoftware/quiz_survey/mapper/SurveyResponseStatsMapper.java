package com.gissoftware.quiz_survey.mapper;

import com.gissoftware.quiz_survey.dto.SurveyResponseStatsDTO;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SurveyResponseStatsMapper {

    public SurveyResponseStatsDTO.OverallStatsDTO toOverallStats(int totalInvited, int totalResponded, double responseRate) {
        return SurveyResponseStatsDTO.OverallStatsDTO.builder()
                .totalInvited(totalInvited)
                .totalResponded(totalResponded)
                .overallResponseRate(responseRate)
                .build();
    }

    public SurveyResponseStatsDTO.BreakdownByRoleDTO toRoleBreakdown(String position, int responded, int invited, double rate) {
        return SurveyResponseStatsDTO.BreakdownByRoleDTO.builder()
                .position(position)
                .responded(responded)
                .invited(invited)
                .responseRate(rate)
                .build();
    }

    public SurveyResponseStatsDTO.BreakdownByOutletDTO toOutletBreakdown(String outlet, int responded, int invited, double rate, List<SurveyResponseStatsDTO.BreakdownByRoleDTO> position) {
        return SurveyResponseStatsDTO.BreakdownByOutletDTO.builder()
                .outlet(outlet)
                .responded(responded)
                .invited(invited)
                .responseRate(rate)
                .position(position)
                .build();
    }

    public SurveyResponseStatsDTO.BreakdownByRegionDTO toRegionBreakdown(String region, int responded, int invited, double rate, List<SurveyResponseStatsDTO.BreakdownByOutletDTO> outlets) {
        return SurveyResponseStatsDTO.BreakdownByRegionDTO.builder()
                .region(region)
                .responded(responded)
                .invited(invited)
                .responseRate(rate)
                .outlets(outlets)
                .build();
    }
}

