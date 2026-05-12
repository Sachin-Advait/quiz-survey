package com.gissoftware.quiz_survey.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {

  private String clientId;
  private boolean is_admin;
  private String userRole;
  private String staff_name;
  private String staff_outlet;
  private String staff_region;
  private String staff_position;
}
