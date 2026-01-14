package com.gissoftware.quiz_survey.service;

import com.gissoftware.quiz_survey.model.NotificationModel;
import com.gissoftware.quiz_survey.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;

  /* ================= GET ALL FOR USER ================= */
  public List<NotificationModel> getUserNotifications(String userId) {
    return notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
  }

  /* ================= MARK ALL AS READ ================= */
  public void markAllAsRead(String userId) {
    List<NotificationModel> notifications =
        notificationRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);

    notifications.forEach(
        n -> {
          n.setDeleted(true);
          n.setRead(true);
        });

    notificationRepository.saveAll(notifications);
  }

  /* ================= MARK ONE AS READ ================= */
  public void markAsRead(String notificationId) {
    notificationRepository
        .findById(notificationId)
        .ifPresent(
            n -> {
              n.setRead(true);
              n.setDeleted(true);
              notificationRepository.save(n);
            });
  }

  /* ================= UNREAD COUNT ================= */
  public long getUnreadCount(String userId) {
    return notificationRepository.countByUserIdAndReadFalseAndDeletedFalse(userId);
  }
}
