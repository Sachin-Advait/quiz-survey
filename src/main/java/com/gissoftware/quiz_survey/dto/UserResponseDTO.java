package com.gissoftware.quiz_survey.dto;

import com.gissoftware.quiz_survey.model.Outlet;
import com.gissoftware.quiz_survey.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {
    private String id;
    private String username;
    private UserRole role;
    private String region;
    private Outlet outlet;
    private Instant createdAt;
}
