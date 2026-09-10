package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.StaffLastLoginDTO;
import com.gissoftware.quiz_survey.dto.StaffLoginCountDTO;
import com.gissoftware.quiz_survey.repository.StaffLoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffLoginService {

  private final StaffLoginRepository staffLoginRepository;

  public Page<StaffLastLoginDTO> getLastLogins(
      String search,
      String type,
      Integer day,
      Integer month,
      Integer year,
      String date,
      int page,
      int size) {
    return staffLoginRepository.getLastLogins(search, type, day, month, year, date, page, size);
  }

  public Page<StaffLoginCountDTO> getLoginCounts(
      String search,
      String type,
      Integer day,
      Integer month,
      Integer year,
      String date,
      int page,
      int size) {

    return staffLoginRepository.getLoginCounts(search, type, day, month, year, date, page, size);
  }
}
