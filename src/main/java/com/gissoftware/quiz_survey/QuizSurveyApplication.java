package com.gissoftware.quiz_survey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

@SpringBootApplication
public class QuizSurveyApplication implements ApplicationListener<WebServerInitializedEvent> {

  public static void main(String[] args) {
    SpringApplication.run(QuizSurveyApplication.class, args);
  }

  @Override
  public void onApplicationEvent(WebServerInitializedEvent event) {
    int port = event.getWebServer().getPort();
    System.out.println("Server Started At PORT: " + port);
  }
}
