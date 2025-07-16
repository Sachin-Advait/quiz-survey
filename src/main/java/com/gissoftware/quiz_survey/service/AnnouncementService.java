package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.AnnouncementWithReadStatus;
import com.gissoftware.quiz_survey.model.Announcement;
import com.gissoftware.quiz_survey.model.AnnouncementRead;
import com.gissoftware.quiz_survey.model.QuizSurveyModel;
import com.gissoftware.quiz_survey.repository.AnnouncementReadRepository;
import com.gissoftware.quiz_survey.repository.AnnouncementRepository;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepo;
    private final AnnouncementReadRepository readRepo;
    private final QuizSurveyRepository quizSurveyRepository;

    public Announcement create(String quizSurveyId, String message) {

        QuizSurveyModel quizSurveyModel = quizSurveyRepository.findById(quizSurveyId).orElseThrow(()
                -> new RuntimeException("Invalid quiz survey Id"));

        return announcementRepo.save(Announcement.builder()
                .title(quizSurveyModel.getTitle())
                .message(message)
                .build());
    }

    public List<AnnouncementWithReadStatus> getAllWithReadStatus(String userId) {
        Set<String> readIds = readRepo.findById(userId)
                .map(AnnouncementRead::getAnnouncementIds)
                .orElse(Set.of());

        return announcementRepo.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(a -> new AnnouncementWithReadStatus(
                        a.getId(),
                        a.getTitle(),
                        a.getMessage(),
                        a.getCreatedAt(),
                        readIds.contains(a.getId()) // true or false
                ))
                .toList();
    }


    public void markAsRead(String userId, String announcementId) {
        AnnouncementRead read = readRepo.findById(userId)
                .orElse(AnnouncementRead.builder().userId(userId).build());

        read.getAnnouncementIds().add(announcementId);
        readRepo.save(read);
    }

}
