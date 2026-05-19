package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.CreateGroupRequest;
import com.gissoftware.quiz_survey.model.UserGroupModel;
import com.gissoftware.quiz_survey.service.UserGroupService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/groups")
@RequiredArgsConstructor
public class UserGroupController {

  private final UserGroupService service;

  // CREATE
  @PostMapping
  public UserGroupModel createGroup(@RequestBody CreateGroupRequest request) {
    return service.createGroup(request);
  }

  // GET ALL
  @GetMapping
  public List<UserGroupModel> getAllGroups() {
    return service.getAllGroups();
  }

  // GET ONE
  @GetMapping("/{groupId}")
  public UserGroupModel getGroupById(@PathVariable String groupId) {
    return service.getGroupById(groupId);
  }

  // UPDATE
  @PutMapping("/{groupId}")
  public UserGroupModel updateGroup(
      @PathVariable String groupId, @RequestBody CreateGroupRequest request) {
    return service.updateGroup(groupId, request);
  }

  // DELETE
  @DeleteMapping("/{groupId}")
  public String deleteGroup(@PathVariable String groupId) {
    service.deleteGroup(groupId);
    return "Group deleted successfully";
  }

  // ADD USERS
  @PostMapping("/{groupId}/users")
  public UserGroupModel addUsers(@PathVariable String groupId, @RequestBody List<String> userIds) {
    return service.addUsers(groupId, userIds);
  }

  // REMOVE USERS
  @DeleteMapping("/{groupId}/users")
  public UserGroupModel removeUsers(
      @PathVariable String groupId, @RequestBody List<String> userIds) {
    return service.removeUsers(groupId, userIds);
  }
}
