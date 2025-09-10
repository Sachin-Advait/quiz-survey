package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.AnnouncementMode;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface QuizSurveyRepository extends MongoRepository<QuizSurveyModel, String> {
    List<QuizSurveyModel> findByAnnouncementModeAndScheduledTimeBeforeAndIsAnnouncedFalse(
            AnnouncementMode mode, Instant time);
}
