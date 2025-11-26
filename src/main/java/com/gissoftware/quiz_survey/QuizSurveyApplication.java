package com.gissoftware.quiz_survey;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;

@SpringBootApplication
public class QuizSurveyApplication implements ApplicationListener<WebServerInitializedEvent> {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        // Set system properties for Spring Boot
        setSystemProperty("DB_URL", dotenv);
        setSystemProperty("PORT", dotenv);
        setSystemProperty("KONG_SECRET", dotenv);

        SpringApplication.run(QuizSurveyApplication.class, args);
    }

    private static void setSystemProperty(String key, Dotenv dotenv) {
        String value = dotenv.get(key);
        if (value != null) {
            System.setProperty(key, value);
        }
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        System.out.println("Server Started At PORT: " + port);
    }
}
