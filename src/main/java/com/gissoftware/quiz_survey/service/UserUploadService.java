package com.gissoftware.quiz_survey.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gissoftware.quiz_survey.controller.UserUploadController;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
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

    private final int integerLikeCount = 0;

    @Autowired
    private MongoTemplate mongoTemplate;

    public UserUploadController.UploadResponse uploadUsers(
            InputStream inputStream,
            String filename,
            String quarter,
            Integer year) throws IOException {

        System.out.println("\n=== Starting Upload ===");
        System.out.println("Quarter: " + quarter);
        System.out.println("Year: " + year);
        System.out.println("File: " + filename + "\n");

        List<Map<String, Object>> rawData = readInputFile(filename, inputStream);
        System.out.println("✓ Read " + rawData.size() + " rows");

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

        if (users.isEmpty()) {
            System.out.println("✗ No valid users to upload");
            return UserUploadController.UploadResponse.builder()
                    .success(false)
                    .message("No valid users to upload")
                    .validationErrors(validationErrors)
                    .build();
        }

        return uploadUsersToMongoDB(users, validationErrors);
    }

    /* ====================== INDEX CLEANUP ====================== */
    private void dropUniqueIndexOnStaffId() {
        try {
            mongoTemplate.indexOps("users").dropIndex("staffId");
            System.out.println("✓ Dropped unique index on staffId");
        } catch (Exception e) {
            System.out.println("ℹ️ staffId unique index not found");
        }
    }

    private void dropDuplicateStaffIdIndex() {
        try {
            mongoTemplate.indexOps("users").dropIndex("staffId_1");
            System.out.println("✓ Dropped duplicate staffId index");
        } catch (Exception e) {
            System.out.println("ℹ️ staffId_1 index not found");
        }
    }

    private void ensureCompositeUniqueIndex() {
        mongoTemplate.indexOps("users").createIndex(
                new Index()
                        .on("staffId", Sort.Direction.ASC)
                        .on("quarter", Sort.Direction.ASC)
                        .on("year", Sort.Direction.ASC)
                        .unique()
        );
        mongoTemplate.indexOps("users").createIndex(
                new Index().on("quarter", Sort.Direction.ASC).on("year", Sort.Direction.ASC)
        );
    }

    /* ====================== CORE UPLOAD ====================== */

    private UserUploadController.UploadResponse uploadUsersToMongoDB(
            List<Map<String, Object>> users,
            List<UserUploadController.UploadError> validationErrors) {

        System.out.println("✓ Connected to MongoDB");

        // Clean wrong indexes (do once per upload run)
        dropUniqueIndexOnStaffId();
        dropDuplicateStaffIdIndex();
        ensureCompositeUniqueIndex();

        // Deactivate all existing users
        mongoTemplate.updateMulti(new Query(), new Update().set("activeUser", false), "users");
        System.out.println("✓ All existing users deactivated");

        int insertedCount = 0;
        int updatedCount = 0;
        int errorCount = 0;
        List<UserUploadController.UploadError> errors = new ArrayList<>(validationErrors);

        for (Map<String, Object> user : users) {
            try {
                String staffId = (String) user.get("staffId");
                String quarter = (String) user.get("quarter");
                Integer year = (Integer) user.get("year");

                Query query = new Query(
                        Criteria.where("staffId").is(staffId)
                                .and("quarter").is(quarter)
                                .and("year").is(year)
                );

                user.put("activeUser", true);

                Update update = new Update();
                for (Map.Entry<String, Object> entry : user.entrySet()) {
                    if (!Set.of("staffId", "quarter", "year").contains(entry.getKey())) {
                        update.set(entry.getKey(), entry.getValue());
                    }
                }

                update.setOnInsert("createdAt", new Date());

                Document existing = mongoTemplate.findOne(query, Document.class, "users");
                mongoTemplate.upsert(query, update, "users");

                if (existing == null) insertedCount++;
                else updatedCount++;

            } catch (Exception e) {
                errorCount++;
                errors.add(new UserUploadController.UploadError(
                        String.valueOf(user.get("staffId")),
                        e.getMessage()
                ));
            }
        }

        System.out.println("\n=== Upload Summary ===");
        System.out.println("Inserted: " + insertedCount);
        System.out.println("Updated: " + updatedCount);
        System.out.println("Errors: " + errorCount);
        System.out.println("Total integer-like numeric cells: " + integerLikeCount);

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

    /* ====================== FILE READERS ====================== */

    private List<Map<String, Object>> readInputFile(String filename, InputStream inputStream) throws IOException {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        if (extension.equals("xlsx")) return readExcelFile(inputStream);
        if (extension.equals("json")) return readJsonFile(inputStream);
        throw new IllegalArgumentException("Unsupported file type: " + extension);
    }

    private List<Map<String, Object>> readExcelFile(InputStream inputStream) throws IOException {
        List<Map<String, Object>> data = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) headers.add(cell.getStringCellValue());

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, Object> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell != null) rowData.put(headers.get(j), getCellValue(cell));
                }
                data.add(rowData);
            }
        }
        return data;
    }

    private Object getCellValue(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) yield cell.getDateCellValue();
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) yield String.valueOf((long) d);
                else yield d;
            }
            case BOOLEAN -> cell.getBooleanCellValue();
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private List<Map<String, Object>> readJsonFile(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputStream, new TypeReference<>() {
        });
    }

    /* ====================== VALIDATION ====================== */

    private Map<String, Object> validateAndCleanUser(Map<String, Object> row, String quarter, Integer year) {
        Map<String, Object> user = new HashMap<>();

        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.equalsIgnoreCase("staffId") || key.equalsIgnoreCase("Staff_ID")) {
                user.put("staffId", String.valueOf(value).trim());
            } else if (key.equalsIgnoreCase("username") || key.equalsIgnoreCase("STAFF_NAME")) {
                user.put("username", String.valueOf(value).trim());
            } else if (key.equalsIgnoreCase("role") || key.equalsIgnoreCase("ADMIN")) {
                user.put("role", convertAdminToRole(String.valueOf(value)));
            } else if (key.equalsIgnoreCase("region")) {
                user.put("region", String.valueOf(value).trim());
            } else if (key.equalsIgnoreCase("outlet")) {
                user.put("outlet", String.valueOf(value).trim());
            } else if (key.equalsIgnoreCase("position")) {
                user.put("position", String.valueOf(value).trim());
            } else if (key.equalsIgnoreCase("activeUser")) {
                user.put("activeUser", convertActiveUser(value));
            } else {
                user.put(key.toLowerCase(), value);
            }
        }

        user.put("quarter", quarter);
        user.put("year", year);
        user.put("updatedAt", new Date());

        return user;
    }

    private String convertAdminToRole(String adminValue) {
        if (adminValue == null || adminValue.isEmpty()) return "USER";
        String normalized = adminValue.trim().toUpperCase();
        return Arrays.asList("TRUE", "YES", "1", "ADMIN").contains(normalized) ? "ADMIN" : "USER";
    }

    private boolean convertActiveUser(Object activeValue) {
        if (activeValue == null) return true;
        return !String.valueOf(activeValue).trim().equalsIgnoreCase("no");
    }
}