package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.UserResponseDTO;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.model.UserRole;
import com.gissoftware.quiz_survey.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
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
                .channel(user.getChannel())
                .quarter(user.getQuarter())
                .year(user.getYear())
                .activeUser(user.getActiveUser())
                .build();
    }

    public UserModel syncUser(UserModel user) {
        Optional<UserModel> userModel = userRepository.findByStaffIdAndActiveUserTrue(user.getStaffId());
        Optional<UserModel> inactiveUser = userRepository.findByStaffId(user.getStaffId());

        if (userModel.isPresent()) {
            userModel.get().setFcmToken(user.getFcmToken());
            userRepository.save(userModel.get());
            return userModel.get();
        } else if (inactiveUser.isPresent()) {
            inactiveUser.get().setFcmToken(user.getFcmToken());
            userRepository.save(inactiveUser.get());
            return inactiveUser.get();
        }

        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int quarterNum = (month - 1) / 3 + 1;

        if (user.getQuarter() == null) {
            user.setQuarter("Q" + quarterNum);
        }

        if (user.getYear() == null) {
            user.setYear(now.getYear());
        }

        // ✅ Defaults
        if (user.getRole() == null) {
            user.setRole(UserRole.USER);
        }

        if (user.getActiveUser() == null) {
            user.setActiveUser(true);
        }
        return userRepository.save(user);
    }

    public List<UserResponseDTO> getAllUsers(
            String region, String outlet, Integer page, Integer size) {
        List<UserModel> users;

        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserModel> pagedUsers;

            if (region != null && outlet != null) {
                pagedUsers = userRepository.findByRegionAndOutletAndActiveUserTrue(region, outlet, pageable);
            } else if (region != null) {
                pagedUsers = userRepository.findByRegionAndActiveUserTrue(region, pageable);
            } else if (outlet != null) {
                pagedUsers = userRepository.findByOutletAndActiveUserTrue(outlet, pageable);
            } else {
                pagedUsers = userRepository.findByActiveUserTrue(pageable);
            }
            users = pagedUsers.getContent();
        } else {
            if (region != null && outlet != null) {
                users = userRepository.findByRegionAndOutletAndActiveUserTrue(region, outlet);
            } else if (region != null) {
                users = userRepository.findByRegionAndActiveUserTrue(region);
            } else if (outlet != null) {
                users = userRepository.findByOutletAndActiveUserTrue(outlet);
            } else {
                users = userRepository.findByActiveUserTrue();
            }
        }

        return users.stream().map(this::toDto).collect(Collectors.toList());
    }

    public UserModel updateUser(String id, UserModel request) {
        UserModel user =
                userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

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

    public List<String> getAllRegions() {
        List<UserModel> users = userRepository.findAllRegionsOfActiveUsers();

        Set<String> regionSet =
                users.stream()
                        .map(UserModel::getRegion)
                        .filter(Objects::nonNull)
                        .map(String::toLowerCase)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

        List<String> regions = new ArrayList<>();
        regions.add("all");
        regions.addAll(regionSet);

        return regions;
    }

    public String getUserNameById(String userId) {
        return userRepository.findById(userId).map(UserModel::getUsername).orElse("Unknown User");
    }
}
