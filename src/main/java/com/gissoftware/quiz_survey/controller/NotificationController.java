package com.gissoftware.quiz_survey.controller;

import com.gissoftware.quiz_survey.model.NotificationModel;
import com.gissoftware.quiz_survey.service.NotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  /* ================= GET USER NOTIFICATIONS ================= */
  @GetMapping("/{userId}")
  public List<NotificationModel> getNotifications(@PathVariable String userId) {
    return notificationService.getUserNotifications(userId);
  }

  /* ================= MARK ALL AS READ ================= */
  @PutMapping("/read/{userId}")
  public void markAllAsRead(@PathVariable String userId) {
    notificationService.markAllAsRead(userId);
  }

  /* ================= MARK ONE AS READ ================= */
  @PutMapping("/read-one/{id}")
  public void markOneAsRead(@PathVariable String id) {
    notificationService.markAsRead(id);
  }

  /* ================= UNREAD COUNT ================= */
  @GetMapping("/unread-count/{userId}")
  public long unreadCount(@PathVariable String userId) {
    return notificationService.getUnreadCount(userId);
  }
}
