package com.gissoftware.quiz_survey.dto;

import com.gissoftware.quiz_survey.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private String id;
    private String username;
    private UserRole role;


}
