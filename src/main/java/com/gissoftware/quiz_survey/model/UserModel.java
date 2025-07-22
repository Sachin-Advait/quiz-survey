package com.gissoftware.quiz_survey.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EnableMongoAuditing

public class UserModel {
    @Id
    private String id;
    @Indexed(unique = true)
    @NonNull
    private String username;
    private String password;
    private UserRole role;
    private Region region;
    private Outlet outlet;

    @CreatedDate
    private Instant createdAt;
}
