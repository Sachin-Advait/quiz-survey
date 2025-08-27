package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.UserResponseDTO;
import com.gissoftware.quiz_survey.model.ResponseModel;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.QuizSurveyRepository;
import com.gissoftware.quiz_survey.repository.ResponseRepo;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class ActivityService {

    private final ResponseRepo responseRepo;
    private final UserRepository userRepository;
    private final QuizSurveyRepository quizSurveyRepo;

    // Users in a region who didn't respond
    public List<UserResponseDTO> getNonRespondedUsersByRegion(String surveyId, String region) {
        // All responses for this survey
        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(surveyId);
        Set<String> respondedUserIds = responses.stream()
                .map(ResponseModel::getUserId)
                .collect(Collectors.toSet());

        // All invited users for the survey
        List<UserModel> invitedUsers = quizSurveyRepo.findById(surveyId)
                .map(survey -> userRepository.findAllById(survey.getTargetedUsers()))
                .orElse(List.of());

        // Filter users in the requested region who didn't respond
        return invitedUsers.stream()
                .filter(u -> region.equals(u.getRegion()))
                .filter(u -> !respondedUserIds.contains(u.getId()))
                .map(user -> UserResponseDTO.builder()
                        .id(user.getId())
                        .staffId(user.getStaffId())
                        .username(user.getUsername())
                        .role(user.getRole())
                        .region(user.getRegion())
                        .outlet(user.getOutlet())
                        .position(user.getPosition())
                        .build())
                .toList();
    }

    // Users in an outlet who didn't respond
    public List<UserResponseDTO> getNonRespondedUsersByOutlet(String surveyId, String outlet) {
        List<ResponseModel> responses = responseRepo.findByQuizSurveyId(surveyId);
        Set<String> respondedUserIds = responses.stream()
                .map(ResponseModel::getUserId)
                .collect(Collectors.toSet());

        List<UserModel> invitedUsers = quizSurveyRepo.findById(surveyId)
                .map(survey -> userRepository.findAllById(survey.getTargetedUsers()))
                .orElse(List.of());

        return invitedUsers.stream()
                .filter(u -> outlet.equals(u.getOutlet()))
                .filter(u -> !respondedUserIds.contains(u.getId()))
                .map(user -> UserResponseDTO.builder()
                        .id(user.getId())
                        .staffId(user.getStaffId())
                        .username(user.getUsername())
                        .role(user.getRole())
                        .region(user.getRegion())
                        .outlet(user.getOutlet())
                        .position(user.getPosition())
                        .build())
                .toList();
    }
}
