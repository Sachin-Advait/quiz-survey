package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.Announcement;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AnnouncementRepository extends MongoRepository<Announcement, String> {
}
