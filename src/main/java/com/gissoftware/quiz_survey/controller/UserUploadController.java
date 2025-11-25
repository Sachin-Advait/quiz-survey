package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.service.UserUploadService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserUploadController {

    @Autowired
    private UserUploadService userUploadService;

    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFromResources() {
        try {
            String filename = "master_4_2025.xlsx";

            // Parse quarter and year
            String[] parts = filename.replace(".xlsx", "").split("_");
            String quarter = "Q" + parts[1];
            Integer year = Integer.parseInt(parts[2]);

            InputStream inputStream = getClass().getClassLoader()
                    .getResourceAsStream(filename);

            if (inputStream == null) {
                throw new RuntimeException("File not found: " + filename);
            }

            UploadResponse response = userUploadService.uploadUsers(
                    inputStream,
                    filename,
                    quarter,
                    year
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    UploadResponse.builder()
                            .success(false)
                            .message("Error: " + e.getMessage())
                            .build()
            );
        }
    }


    @Data
    @Builder
    public static class UploadResponse {

        private boolean success;
        private String message;
        private int insertedCount;
        private int updatedCount;
        private int errorCount;
        private List<UploadError> errors;
        private List<UploadError> validationErrors;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadError {

        private Object identifier;
        private String error;

        public UploadError(int rowNumber, String error) {
            this.identifier = rowNumber;
            this.error = error;
        }

        public UploadError(String staffId, String error) {
            this.identifier = staffId;
            this.error = error;
        }
    }
}

