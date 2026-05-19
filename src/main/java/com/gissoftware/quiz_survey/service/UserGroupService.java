package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.dto.CreateGroupRequest;
import com.gissoftware.quiz_survey.model.UserGroupModel;
import com.gissoftware.quiz_survey.model.UserModel;
import com.gissoftware.quiz_survey.repository.UserGroupRepository;
import com.gissoftware.quiz_survey.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserGroupService {

  private final UserRepository userRepository;
  private final UserGroupRepository groupRepository;

  // CREATE
  public UserGroupModel createGroup(CreateGroupRequest request) {

    List<UserModel> users = userRepository.findAllById(request.getUserIds());

    List<UserGroupModel.GroupUser> groupUsers = mapUsers(users);

    UserGroupModel group =
        UserGroupModel.builder()
            .name(request.getName())
            .description(request.getDescription())
            .users(groupUsers)
            .active(true)
            .build();

    return groupRepository.save(group);
  }

  // GET ALL
  public List<UserGroupModel> getAllGroups() {
    return groupRepository.findAll();
  }

  // GET ONE
  public UserGroupModel getGroupById(String groupId) {

    return groupRepository
        .findById(groupId)
        .orElseThrow(() -> new RuntimeException("Group not found"));
  }

  // UPDATE
  // UPDATE
  public UserGroupModel updateGroup(String groupId, CreateGroupRequest request) {

    UserGroupModel existing = getGroupById(groupId);

    // update only if not null
    if (request.getName() != null) {
      existing.setName(request.getName());
    }

    if (request.getDescription() != null) {
      existing.setDescription(request.getDescription());
    }

    // update users only if provided
    if (request.getUserIds() != null) {

      List<UserModel> users = userRepository.findAllById(request.getUserIds());

      List<UserGroupModel.GroupUser> groupUsers = mapUsers(users);

      existing.setUsers(groupUsers);
    }

    return groupRepository.save(existing);
  }

  // DELETE
  public void deleteGroup(String groupId) {

    UserGroupModel group = getGroupById(groupId);

    groupRepository.delete(group);
  }

  // ADD USERS
  public UserGroupModel addUsers(String groupId, List<String> userIds) {

    UserGroupModel group = getGroupById(groupId);

    List<UserModel> users = userRepository.findAllById(userIds);

    List<UserGroupModel.GroupUser> newUsers = mapUsers(users);

    if (group.getUsers() == null) {
      group.setUsers(new ArrayList<>());
    }

    group.getUsers().addAll(newUsers);

    return groupRepository.save(group);
  }

  // REMOVE USERS
  public UserGroupModel removeUsers(String groupId, List<String> userIds) {

    UserGroupModel group = getGroupById(groupId);

    group.getUsers().removeIf(user -> userIds.contains(user.getUserId()));

    return groupRepository.save(group);
  }

  // COMMON USER MAPPER
  private List<UserGroupModel.GroupUser> mapUsers(List<UserModel> users) {

    return users.stream()
        .map(
            user ->
                UserGroupModel.GroupUser.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .staffId(user.getStaffId())
                    .region(user.getRegion())
                    .outlet(user.getOutlet())
                    .mobile(user.getMobile())
                    .build())
        .toList();
  }
}
