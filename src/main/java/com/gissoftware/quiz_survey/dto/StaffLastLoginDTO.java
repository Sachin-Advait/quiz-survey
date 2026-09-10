package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffLastLoginDTO {

  private String staffId;
  private String name;
  private String date;
  private String time;
}
