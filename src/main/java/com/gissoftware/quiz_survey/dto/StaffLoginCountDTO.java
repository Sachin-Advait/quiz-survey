package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffLoginCountDTO {

  private String staffId;
  private String username;
  private Long loginCount;
}
