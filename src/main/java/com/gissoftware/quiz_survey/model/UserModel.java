package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    private String staffId;
    private String username;
    private UserRole role;
    private String region;
    private String outlet;
    private String position;

    @CreatedDate
    private Instant createdAt;
}
