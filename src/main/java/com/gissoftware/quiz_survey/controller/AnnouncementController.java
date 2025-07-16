package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.dto.AnnouncementReadRequest;
import com.gissoftware.quiz_survey.dto.AnnouncementRequest;
import com.gissoftware.quiz_survey.dto.AnnouncementWithReadStatus;
import com.gissoftware.quiz_survey.dto.ApiResponseDTO;
import com.gissoftware.quiz_survey.model.Announcement;
import com.gissoftware.quiz_survey.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    // POST /api/admin/announcements — (Admin only)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/announcements")
    public ResponseEntity<ApiResponseDTO<Announcement>> create(@RequestBody AnnouncementRequest req) {
        Announcement saved = announcementService.create(req.getQuizSurveyId(), req.getMessage());
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Announcement posted!", saved));
    }

    // GET /api/announcements — (All users)
    @GetMapping("/announcements")
    public ResponseEntity<ApiResponseDTO<List<AnnouncementWithReadStatus>>> getAll(@RequestParam String userId) {
        List<AnnouncementWithReadStatus> list = announcementService.getAllWithReadStatus(userId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Announcements retrieved successfully", list));
    }

    // PATCH /api/announcements/read — (Mark as read)
    @PatchMapping("/announcements/read")
    public ResponseEntity<ApiResponseDTO<String>> markAsRead(@RequestBody AnnouncementReadRequest req) {
        announcementService.markAsRead(req.getUserId(), req.getAnnouncementId());
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "Marked as read", null));
    }

    @PatchMapping("/announcements/mark-all-read")
    public ResponseEntity<ApiResponseDTO<String>> markAllAsRead(@RequestBody Map<String, String> req) {
        String userId = req.get("userId");
        announcementService.markAllAsRead(userId);
        return ResponseEntity.ok(new ApiResponseDTO<>(true, "All announcements marked as read", null));
    }

}
