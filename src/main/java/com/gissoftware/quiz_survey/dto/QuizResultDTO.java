package com.gissoftware.quiz_survey.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static public class QuestionAnswerDTO {
        private List<OptionDTO> choices;
        private String type;
        private Object selectedOptions;
        private String correctAnswer;
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

