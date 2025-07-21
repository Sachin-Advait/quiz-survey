package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.UserResponseDTO;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.model.UserRole;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDTO toDto(UserModel user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .region(user.getRegion())
                .outlet(user.getOutlet())
                .build();
    }

    public UserModel register(UserModel user) {
        userRepository.findByUsername(user.getUsername())
                .ifPresent(u -> {
                    throw new RuntimeException("Username already exists");
                });

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set default role if not provided
        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }

        return userRepository.save(user);
    }


    public UserModel login(String username, String rawPassword) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getPassword()))
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
    }

    public List<UserResponseDTO> getAllUsers(String region, String outlet) {
        List<UserModel> users;

        if (region != null && outlet != null) {
            users = userRepository.findByRegionAndOutlet(region, outlet);
        } else if (region != null) {
            users = userRepository.findByRegion(region);
        } else if (outlet != null) {
            users = userRepository.findByOutlet(outlet);
        } else {
            users = userRepository.findAll();
        }

        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }


    public UserModel updateUser(String id, UserModel request) {
        UserModel user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getRegion() != null) {
            user.setRegion(request.getRegion());

        }

        if (request.getOutlet() != null) {
            user.setOutlet(request.getOutlet());
        }

        return userRepository.save(user);
    }
}

