package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.*;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.UserRepository;
import com.gissoftware.quiz_survey.service.StaffLoginService;
import com.gissoftware.quiz_survey.service.UserService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final UserRepository userRepository;
  private final StaffLoginService staffLoginService;

  @GetMapping
  public ResponseEntity<ApiResponseDTO<List<UserResponseDTO>>> getAllUsers(
      @RequestParam(required = false) String region,
      @RequestParam(required = false) String outlet,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size) {
    List<UserResponseDTO> response = userService.getAllUsers(region, outlet, page, size);
    return ResponseEntity.ok(new ApiResponseDTO<>(true, "Retrieved all users", response));
  }

  @PostMapping("/sync-user")
  public ResponseEntity<ApiResponseDTO<UserResponseDTO>> syncUser(@RequestBody UserModel user) {
    UserModel registeredUser = userService.syncUser(user);
    return ResponseEntity.ok(
        new ApiResponseDTO<>(true, "Sync user successful", userService.toDto(registeredUser)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponseDTO<UserResponseDTO>> updateUser(
      @PathVariable String id, @RequestBody UserModel request) {

    UserModel updated = userService.updateUser(id, request);
    UserResponseDTO response = userService.toDto(updated);

    return ResponseEntity.ok(new ApiResponseDTO<>(true, "User updated successfully", response));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteUserById(@PathVariable String id) {
    UserModel user = userService.deleteUserById(id);
    if (user == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ApiResponseDTO<>(false, "User not found", null));
    }
    return ResponseEntity.ok(new ApiResponseDTO<>(true, "User deleted successfully", null));
  }

  @GetMapping("/regions")
  public ResponseEntity<ApiResponseDTO<List<String>>> getAllRegions() {
    List<String> regions = userService.getAllRegions();
    return ResponseEntity.ok(new ApiResponseDTO<>(true, "Regions fetched successfully", regions));
  }

  @GetMapping("/user-id/by-client/{staffId}")
  public ResponseEntity<ApiResponseDTO<UserResponseDTO>> getUserIdByClientId(
      @PathVariable String staffId) {

    UserModel user =
        userRepository
            .findByStaffIdAndActiveUserTrue(staffId)
            .or(() -> userRepository.findFirstByStaffIdOrderByYearDescQuarterDesc(staffId))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found for staffId: " + staffId));

    return ResponseEntity.ok(
        new ApiResponseDTO<>(true, "User id fetched successfully", userService.toDto(user)));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(
      @RequestBody LoginRequestDTO request) {

    LoginResponseDTO response = userService.loginByStaffId(request.getStaffId());

    return ResponseEntity.ok(new ApiResponseDTO<>(true, "Login successful", response));
  }

  @GetMapping("/last-login")
  public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getLastLogins(
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "ALL") String type,
      @RequestParam(required = false) Integer day,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) String date,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    if (page < 0) {
      page = 0;
    }

    if (size < 1 || size > 100) {
      size = 20;
    }

    Page<StaffLastLoginDTO> result =
        staffLoginService.getLastLogins(search, type, day, month, year, date, page, size);

    Map<String, Object> data = new LinkedHashMap<>();
    data.put("content", result.getContent());
    data.put("page", result.getNumber());
    data.put("size", result.getSize());
    data.put("totalElements", result.getTotalElements());
    data.put("totalPages", result.getTotalPages());
    data.put("first", result.isFirst());
    data.put("last", result.isLast());

    return ResponseEntity.ok(
        new ApiResponseDTO<>(true, "Staff last login fetched successfully", data));
  }

  @GetMapping("/login-count")
  public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getLoginCounts(
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "ALL") String type,
      @RequestParam(required = false) Integer day,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) String date,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    if (page < 0) {
      page = 0;
    }

    if (size < 1 || size > 100) {
      size = 20;
    }

    Page<StaffLoginCountDTO> result =
        staffLoginService.getLoginCounts(search, type, day, month, year, date, page, size);

    Map<String, Object> data = new LinkedHashMap<>();

    data.put("content", result.getContent());
    data.put("page", result.getNumber());
    data.put("size", result.getSize());
    data.put("totalElements", result.getTotalElements());
    data.put("totalPages", result.getTotalPages());
    data.put("first", result.isFirst());
    data.put("last", result.isLast());

    return ResponseEntity.ok(
        new ApiResponseDTO<>(true, "Staff login counts fetched successfully", data));
  }
}
