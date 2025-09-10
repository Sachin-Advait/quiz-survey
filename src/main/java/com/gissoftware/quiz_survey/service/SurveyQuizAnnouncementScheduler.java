package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.model.AnnouncementMode;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SurveyQuizAnnouncementScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SurveyQuizAnnouncementScheduler.class);
    private final QuizSurveyRepository quizSurveyRepository;

    @Scheduled(fixedRate = 60000)  // Every 1 minute
    public void checkAndAnnounceScheduledQuizzes() {

        List<QuizSurveyModel> scheduledQuizzes = quizSurveyRepository.
                findByAnnouncementModeAndScheduledTimeBeforeAndIsAnnouncedFalse(AnnouncementMode.SCHEDULED, Instant.now());

        for (QuizSurveyModel quiz : scheduledQuizzes) {
            quiz.setIsAnnounced(true);
            quizSurveyRepository.save(quiz);
        }
    }
}
