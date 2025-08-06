package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.UserResponseDTO;
import com.gissoftware.quiz_survey.model.Region;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.model.UserRole;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponseDTO toDto(UserModel user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .staffId(user.getStaffId())
                .username(user.getUsername())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .region(user.getRegion())
                .outlet(user.getOutlet())
                .position(user.getPosition())
                .build();
    }

    public UserModel syncUser(UserModel user) {
        Optional<UserModel> userModel = userRepository.findByStaffId(user.getStaffId());

        if (userModel.isPresent()) {
            return userModel.get();
        }

        // Set default role if not provided
        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }
        return userRepository.save(user);
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


    public UserModel deleteUserById(String id) {
        Optional<UserModel> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            userRepository.deleteById(id);
            return userOpt.get();
        }
        return null;
    }

    public List<Region> getOmanRegions() {
        return List.of(
                new Region("DA", "AL_DAKHILIYAH"),
                new Region("ZU", "DHOFAR")
        );
    }
}

