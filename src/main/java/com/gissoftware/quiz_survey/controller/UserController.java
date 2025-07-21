package com.gissoftware.quiz_survey.controller;


import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.dto.UserResponseDTO;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<UserResponseDTO>>> getAllUsers(
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String outlet
    ) {
        List<UserResponseDTO> response = userService.getAllUsers(region, outlet);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Retrieved all users", response));
    }


    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateUser(
            @PathVariable String id,
            @RequestBody UserModel request) {

        UserModel updated = userService.updateUser(id, request);
        UserResponseDTO response = userService.toDto(updated);

        return ResponseEntity.ok(new ApiResponseDTO<>(true, "User updated successfully", response));
    }
}
