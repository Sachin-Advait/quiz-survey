package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
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

    private String username;
    private String password;
    private UserRole role;
    private Region region;
    private Outlet outlet;

    @CreatedDate
    private Instant createdAt;
}
