package com.odontogestion.api.controller;

import com.odontogestion.api.dto.InAppNotificationResponseDTO;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.security.AuthenticationFacade;
import com.odontogestion.api.service.InAppNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationService inAppNotificationService;
    private final com.odontogestion.api.service.NotificationService notificationService;
    private final AuthenticationFacade authenticationFacade;

    @GetMapping
    public ResponseEntity<List<InAppNotificationResponseDTO>> getUserNotifications() {
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        log.info("Fetching in-app notifications for user ID: {}", authenticatedUser.getId());

        List<InAppNotificationResponseDTO> notifications = inAppNotificationService.getUserNotifications(authenticatedUser);
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        long unreadCount = inAppNotificationService.getUnreadCount(authenticatedUser);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<InAppNotificationResponseDTO> markAsRead(@PathVariable Long id) {
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        log.info("Marking notification ID: {} as read for user ID: {}", id, authenticatedUser.getId());

        InAppNotificationResponseDTO updated = inAppNotificationService.markAsRead(id, authenticatedUser);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead() {
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        log.info("Marking all notifications as read for user ID: {}", authenticatedUser.getId());

        inAppNotificationService.markAllAsRead(authenticatedUser);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    @org.springframework.web.bind.annotation.PostMapping("/test")
    public ResponseEntity<Map<String, String>> sendTestNotification() {
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        log.info("Sending test notification for user ID: {}", authenticatedUser.getId());

        notificationService.sendDailySummaryNotification(authenticatedUser, 3);
        return ResponseEntity.ok(Map.of("message", "Test notification sent successfully"));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(@PathVariable Long id) {
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        log.info("Deleting notification ID: {} for user ID: {}", id, authenticatedUser.getId());

        inAppNotificationService.deleteNotification(id, authenticatedUser);
        return ResponseEntity.ok(Map.of("message", "Notification deleted successfully"));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/clear-read")
    public ResponseEntity<Map<String, String>> clearReadNotifications() {
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        log.info("Clearing read notifications for user ID: {}", authenticatedUser.getId());

        inAppNotificationService.clearReadNotifications(authenticatedUser);
        return ResponseEntity.ok(Map.of("message", "Read notifications cleared successfully"));
    }
}
