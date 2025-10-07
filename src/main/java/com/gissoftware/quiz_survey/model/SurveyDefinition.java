package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SurveyDefinition {
    private List<Page> pages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Page {
        private List<Element> elements;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Element {
        private String type;
        private String name;
        private String title;
        private String arabicTitle;
        private List<String> choices;
        private Object correctAnswer;
    }
}
