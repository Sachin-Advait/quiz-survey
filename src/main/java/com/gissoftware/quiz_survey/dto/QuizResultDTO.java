package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultDTO {
    private String username;
    private Integer score;
    private Integer maxScore;
    private Instant submittedAt;
    private Map<String, QuestionAnswerDTO> answers;


    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static public class QuestionAnswerDTO {
        private List<OptionDTO> options;
        private List<String> selectedOptions;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    static public class OptionDTO {
        private String text;
        private boolean isCorrect;
    }

}

