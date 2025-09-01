package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.*;
import com.gissoftware.quiz_survey.model.Announcement;
import com.gissoftware.quiz_survey.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    // POST /api/admin/announcements — (Admin only)
    @PostMapping("/admin/announcements")
    public ResponseEntity<ApiResponseDTO<Announcement>> create(@RequestBody AnnouncementRequest req) {
        Announcement saved = announcementService.create(req.getQuizSurveyId(), req.getMessage());
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Announcement posted!", saved));
    }

    // GET /api/announcements — (All users)
    @GetMapping("/user/announcements")
    public ResponseEntity<ApiResponseDTO<List<AnnouncementWithReadStatus>>> getAll(@RequestParam String userId) {
        List<AnnouncementWithReadStatus> list = announcementService.getAllWithReadStatus(userId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Announcements retrieved successfully", list));
    }

    // PATCH /api/announcements/read — (Mark as read)
    @PutMapping("/user/announcements/read")
    public ResponseEntity<ApiResponseDTO<String>> markAsRead(@RequestBody AnnouncementReadRequest req) {
        announcementService.markAsRead(req.getUserId(), req.getAnnouncementId());
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Marked as read", null));
    }

    @PutMapping("/user/announcements/mark-all-read")
    public ResponseEntity<ApiResponseDTO<String>> markAllAsRead(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        announcementService.markAllAsRead(userId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "All announcements marked as read", null));
    }

    @PostMapping("/admin/announcements-with-targets")
    public ResponseEntity<ApiResponseDTO<Announcement>> create(@RequestBody CreateAnnouncementRequest request) {
        Announcement announcement = announcementService.createWithTargets(
                request.getQuizSurveyId(),
                request.getMessage(),
                request.getTargetUser()
        );

        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Announcement posted!", announcement));
    }
}
