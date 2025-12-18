package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.ClientUserMappingDTO;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserClientService {

  private final UserRepository userRepository;

  public ClientUserMappingDTO getUserIdByClientId(String staffId) {

    UserModel user =
        userRepository
            .findByStaffId(staffId)
            .orElseThrow(
                () -> new IllegalArgumentException("User not found for staffId: " + staffId));

    return new ClientUserMappingDTO(staffId, user.getId());
  }
}
