package com.gissoftware.quiz_survey.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "device_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FCMToken {

    @Id
    private String token;
    private String userId;
}

