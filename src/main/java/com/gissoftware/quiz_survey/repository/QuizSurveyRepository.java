package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.AnnouncementMode;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuizSurveyRepository extends MongoRepository<QuizSurveyModel, String> {
    List<QuizSurveyModel> findByAnnouncementModeAndScheduledTimeBeforeAndIsAnnouncedFalse(
            AnnouncementMode mode, Instant time);
    // 🔹 REQUIRED FOR USER SCORE

    // Get all quizzes
    List<QuizSurveyModel> findByTypeIgnoreCase(String type);

    // Get quizzes/surveys targeted to a user
    List<QuizSurveyModel> findByTargetedUsersContaining(String userId);

    // Get surveys targeted to a user
    List<QuizSurveyModel> findByTypeIgnoreCaseAndTargetedUsersContaining(
            String type, String userId
    );
}
