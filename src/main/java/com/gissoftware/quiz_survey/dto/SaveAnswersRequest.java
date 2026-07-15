package com.gissoftware.quiz_survey.dto;

import java.util.Map;
import lombok.Data;

@Data
public class SaveAnswersRequest {
    private String userId;
    private Map<String, Object> answers;
    private Integer currentPageNo;
    private String platform;
    private String client;
}