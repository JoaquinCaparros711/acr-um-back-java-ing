package com.odontogestion.api.service;

import com.odontogestion.api.dto.InAppNotificationResponseDTO;
import com.odontogestion.api.entity.InAppNotification;
import com.odontogestion.api.entity.NotificationType;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.repository.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InAppNotificationService implements NotificationService {

    private final InAppNotificationRepository notificationRepository;

    @Override
    @Transactional
    public void sendDailySummaryNotification(User user, int appointmentCount) {
        if (user == null) {
            log.warn("Cannot create in-app daily summary notification: User is null");
            return;
        }

        String title = "Agenda del Día";
        String message = appointmentCount == 1
                ? "Tienes 1 turno programado para hoy."
                : "Tienes " + appointmentCount + " turnos programados para hoy.";

        createInAppNotification(user, title, message, NotificationType.DAILY_SUMMARY);
    }

    @Override
    @Transactional
    public void sendNotification(User user, String title, String message) {
        if (user == null) {
            log.warn("Cannot create in-app notification: User is null");
            return;
        }

        createInAppNotification(user, title, message, NotificationType.SYSTEM);
    }

    @Transactional
    public InAppNotification createInAppNotification(User user, String title, String message, NotificationType type) {
        InAppNotification notification = InAppNotification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type != null ? type : NotificationType.SYSTEM)
                .isRead(false)
                .build();

        log.info("Persisting in-app notification for user ID: {}", user.getId());
        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<InAppNotificationResponseDTO> getUserNotifications(User user) {
        if (user == null) {
            return List.of();
        }

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        if (user == null) {
            return 0;
        }

        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    @Transactional
    public InAppNotificationResponseDTO markAsRead(Long notificationId, User user) {
        if (user == null || notificationId == null) {
            throw new IllegalArgumentException("User and notification ID must not be null");
        }

        InAppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to notification");
        }

        notification.setRead(true);
        InAppNotification updated = notificationRepository.save(notification);
        return mapToResponseDTO(updated);
    }

    @Transactional
    public void markAllAsRead(User user) {
        if (user == null) {
            return;
        }

        notificationRepository.markAllAsReadByUserId(user.getId());
    }

    @Transactional
    public void deleteNotification(Long id, User user) {
        if (user == null || id == null) {
            return;
        }
        log.info("Deleting notification ID: {} for user ID: {}", id, user.getId());
        notificationRepository.deleteByIdAndUserId(id, user.getId());
    }

    @Transactional
    public void clearReadNotifications(User user) {
        if (user == null) {
            return;
        }
        log.info("Clearing read notifications for user ID: {}", user.getId());
        notificationRepository.deleteReadNotificationsByUserId(user.getId());
    }

    private InAppNotificationResponseDTO mapToResponseDTO(InAppNotification notification) {
        return InAppNotificationResponseDTO.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
