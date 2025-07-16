package com.gissoftware.quiz_survey.repository;

import com.gissoftware.quiz_survey.model.AnnouncementRead;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AnnouncementReadRepository extends MongoRepository<AnnouncementRead, String> {
}
