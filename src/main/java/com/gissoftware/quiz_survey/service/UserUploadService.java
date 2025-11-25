package com.gissoftware.quiz_survey.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gissoftware.quiz_survey.controller.UserUploadController;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class UserUploadService {

    private int integerLikeCount = 0;
    @Autowired
    private MongoTemplate mongoTemplate;

    public UserUploadController.UploadResponse uploadUsers(InputStream inputStream, String filename, String quarter,
                                                           Integer year) throws IOException {
        System.out.println("\n=== Starting Upload ===");
        System.out.println("Quarter: " + quarter);
        System.out.println("Year: " + year);
        System.out.println("File: " + filename + "\n");

        // Step 1: Read file (Excel or JSON)
        List<Map<String, Object>> rawData = readInputFile(filename, inputStream);
        System.out.println("✓ Read " + rawData.size() + " rows");

        // Step 2: Validate + transform
        System.out.println("✓ Validating data...");
        List<Map<String, Object>> users = new ArrayList<>();
        List<UserUploadController.UploadError> validationErrors = new ArrayList<>();

        for (int i = 0; i < rawData.size(); i++) {
            try {
                Map<String, Object> user = validateAndCleanUser(rawData.get(i), quarter, year);
                users.add(user);
            } catch (Exception err) {
                validationErrors.add(new UserUploadController.UploadError(i + 1, err.getMessage()));
            }
        }

        System.out.println("Valid: " + users.size());
        System.out.println("Invalid: " + validationErrors.size());

        if (users.isEmpty()) {
            System.out.println("✗ No valid users to upload");
            return UserUploadController.UploadResponse.builder()
                    .success(false)
                    .message("No valid users to upload")
                    .validationErrors(validationErrors)
                    .build();
        }

        // Step 3: Upload
        return uploadUsersToMongoDB(users, validationErrors);
    }

    private List<Map<String, Object>> readInputFile(String filename, InputStream inputStream) throws IOException {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        if (extension.equals("xlsx")) {
            return readExcelFile(inputStream);
        }
        if (extension.equals("json")) {
            return readJsonFile(inputStream);
        }

        throw new IllegalArgumentException("Unsupported file type: " + extension);
    }

    private List<Map<String, Object>> readExcelFile(InputStream inputStream) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            // Dynamically read headers from the Excel file
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null) {
                        rowData.put(headers.get(j), getCellValue(cell));
                    }
                }
                data.add(rowData);
            }
        }

        System.out.println("✓ Read " + data.size() + " rows from Excel");
        return data;
    }

    private Object getCellValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                integerLikeCount++;
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue();
                }

                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) {
                    yield String.valueOf((long) d);    // removes .0 for ALL integer-like numbers
                } else {
                    yield d;
                }
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private List<Map<String, Object>> readJsonFile(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> data = mapper.readValue(inputStream, new TypeReference<List<Map<String, Object>>>() {
        });
        System.out.println("✓ Read " + data.size() + " records from JSON");
        return data;
    }

    private Map<String, Object> validateAndCleanUser(Map<String, Object> row, String quarter, Integer year) {
        Map<String, Object> user = new HashMap<>();

        // Dynamically copy all fields from the row
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Handle specific transformations
            if (key.equalsIgnoreCase("staffId") || key.equalsIgnoreCase("Staff_ID")) {
                user.put("staffId", String.valueOf(value).trim());
            } else if (key.equalsIgnoreCase("username") || key.equalsIgnoreCase("STAFF_NAME")) {
                user.put("username", String.valueOf(value).trim());
            } else if (key.equalsIgnoreCase("role") || key.equalsIgnoreCase("ADMIN")) {
                user.put("role", convertAdminToRole(String.valueOf(value)));
            } else {
                String value1 = value != null ? String.valueOf(value).trim() : "";
                if (key.equalsIgnoreCase("region") || key.equalsIgnoreCase("REGION")) {
                    user.put("region", value1);
                } else if (key.equalsIgnoreCase("outlet") || key.equalsIgnoreCase("OUTLET")) {
                    user.put("outlet", value1);
                } else if (key.equalsIgnoreCase("position") || key.equalsIgnoreCase("POSITION")) {
                    user.put("position", value1);
                } else if (key.equalsIgnoreCase("activeUser") || key.equalsIgnoreCase("Activeuser")) {
                    user.put("activeUser", convertActiveUser(value));
                } else if (key.equalsIgnoreCase("remark") || key.equalsIgnoreCase("REMARK")) {
                    user.put("remark", value1);
                } else {
                    // For any other columns, store them as-is
                    user.put(key.toLowerCase(), value);
                }
            }
        }

        // Add quarter, year, and updatedAt
        user.put("quarter", quarter);
        user.put("year", year);
        user.put("updatedAt", new Date());

        return user;
    }

    private String convertAdminToRole(String adminValue) {
        if (adminValue == null || adminValue.isEmpty()) {
            return "USER";
        }

        String normalized = adminValue.trim().toUpperCase();
        if (Arrays.asList("TRUE", "YES", "1", "ADMIN").contains(normalized)) {
            return "ADMIN";
        }
        return "USER";
    }

    private boolean convertActiveUser(Object activeValue) {
        if (activeValue == null) {
            return true;
        }
        String value = String.valueOf(activeValue).trim().toLowerCase();
        return !value.equals("no");
    }

    private UserUploadController.UploadResponse uploadUsersToMongoDB(List<Map<String, Object>> users, List<UserUploadController.UploadError> validationErrors) {
        System.out.println("✓ Connected to MongoDB");

        int insertedCount = 0;
        int updatedCount = 0;
        int errorCount = 0;
        List<UserUploadController.UploadError> errors = new ArrayList<>(validationErrors);

        for (Map<String, Object> user : users) {
            try {
                String staffId = (String) user.get("staffId");
                String quarter = (String) user.get("quarter");
                Integer year = (Integer) user.get("year");

                Query query = new Query();
                query.addCriteria(Criteria.where("staffId").is(staffId)
                        .and("quarter").is(quarter)
                        .and("year").is(year));

                Update update = new Update();

                // Dynamically set all fields from the user map
                for (Map.Entry<String, Object> entry : user.entrySet()) {
                    if (!entry.getKey().equals("staffId") &&
                            !entry.getKey().equals("quarter") &&
                            !entry.getKey().equals("year")) {
                        update.set(entry.getKey(), entry.getValue());
                    }
                }

                update.setOnInsert("createdAt", new Date());

                Document existingDoc = mongoTemplate.findOne(query, Document.class, "users");

                mongoTemplate.upsert(query, update, "users");

                if (existingDoc == null) {
                    insertedCount++;
                } else {
                    updatedCount++;
                    System.out.println("↻ Updated: " + staffId + " - " + user.get("username"));
                }
            } catch (Exception e) {
                errorCount++;
                errors.add(new UserUploadController.UploadError(user.get("staffId"), e.getMessage()));
            }
        }

        // Create indexes
        mongoTemplate.indexOps("users").createIndex(
                new Index().on("staffId", org.springframework.data.domain.Sort.Direction.ASC)
                        .on("quarter", org.springframework.data.domain.Sort.Direction.ASC)
                        .on("year", org.springframework.data.domain.Sort.Direction.ASC)
                        .unique()
        );
        mongoTemplate.indexOps("users").createIndex(
                new Index().on("staffId", org.springframework.data.domain.Sort.Direction.ASC)
        );
        mongoTemplate.indexOps("users").createIndex(
                new Index().on("quarter", org.springframework.data.domain.Sort.Direction.ASC)
                        .on("year", org.springframework.data.domain.Sort.Direction.ASC)
        );

        System.out.println("\n=== Upload Summary ===");
        System.out.println("Inserted: " + insertedCount);
        System.out.println("Updated: " + updatedCount);
        System.out.println("Errors: " + errorCount);
        System.out.println("Total integer-like numeric cells: " + integerLikeCount);

        if (!errors.isEmpty()) {
            System.out.println("\nErrors:");
            errors.forEach(e -> System.out.println("  " + e.getIdentifier() + ": " + e.getError()));
        }

        System.out.println("\n=== Upload Complete ===\n");

        return UserUploadController.UploadResponse.builder()
                .success(true)
                .message("Upload completed successfully")
                .insertedCount(insertedCount)
                .updatedCount(updatedCount)
                .errorCount(errorCount)
                .errors(errors)
                .validationErrors(validationErrors)
                .build();
    }
}