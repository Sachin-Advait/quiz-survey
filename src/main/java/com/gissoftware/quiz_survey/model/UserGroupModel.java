package com.gissoftware.quiz_survey.model;

import java.time.Instant;
import java.util.List;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user_groups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserGroupModel {

    @Id
    private String id;

    // group name
    private String name;

    private String description;

    // users with full info
    private List<GroupUser> users;

    private Boolean active;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GroupUser {

        private String userId;

        private String username;

        private String staffId;

        private String region;

        private String outlet;

        private String mobile;
    }
}