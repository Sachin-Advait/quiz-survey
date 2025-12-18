package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.UserResponseDTO;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.model.UserRole;
import com.gissoftware.quiz_survey.repository.UserRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
      userModel.get().setFcmToken(user.getFcmToken());
      userRepository.save(userModel.get());
      return userModel.get();
    }

    // Set default role if not provided
    if (user.getRole() == null) {
      user.setRole(UserRole.USER);
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
        pagedUsers = userRepository.findByRegionAndOutlet(region, outlet, pageable);
      } else if (region != null) {
        pagedUsers = userRepository.findByRegion(region, pageable);
      } else if (outlet != null) {
        pagedUsers = userRepository.findByOutlet(outlet, pageable);
      } else {
        pagedUsers = userRepository.findAll(pageable);
      }
      users = pagedUsers.getContent();
    } else {
      if (region != null && outlet != null) {
        users = userRepository.findByRegionAndOutlet(region, outlet);
      } else if (region != null) {
        users = userRepository.findByRegion(region);
      } else if (outlet != null) {
        users = userRepository.findByOutlet(outlet);
      } else {
        users = userRepository.findAll();
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
        List<UserModel> users = userRepository.findAllRegions();

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
}
