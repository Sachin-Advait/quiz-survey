package com.gissoftware.quiz_survey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuizSurveyApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizSurveyApplication.class, args);
        System.out.println("Server Started At PORT:8080");

    }
}
