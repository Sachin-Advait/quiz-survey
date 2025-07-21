package com.gissoftware.quiz_survey.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum UserRole {
    USER, ADMIN;

    @JsonCreator
    public static UserRole from(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        return UserRole.valueOf(role.toUpperCase());
    }
}

