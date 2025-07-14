package com.gissoftware.quiz_survey.dto;

import com.gissoftware.quiz_survey.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String userId;
    private UserRole role;
    private String token;
}

