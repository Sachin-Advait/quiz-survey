package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.dto.LoginRequest;
import com.gissoftware.quiz_survey.dto.UserResponseDTO;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> register(@RequestBody UserModel user) {
        UserModel registeredUser = userService.register(user);
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Registration successful", userService.toDto(registeredUser))
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> login(@RequestBody LoginRequest loginRequest) {
        UserModel user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Login successful", userService.toDto(user))
        );
    }
}


