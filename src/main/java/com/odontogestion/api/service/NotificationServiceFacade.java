package com.odontogestion.api.service;

import com.odontogestion.api.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class NotificationServiceFacade implements NotificationService {

    private final InAppNotificationService inAppNotificationService;
    private final ExpoPushNotificationService expoPushNotificationService;

    @Override
    public void sendDailySummaryNotification(User user, int appointmentCount) {
        if (user == null) {
            log.warn("Cannot dispatch daily summary notification: User is null");
            return;
        }

        log.info("Dispatching daily summary notification for user ID: {} with {} appointments", user.getId(), appointmentCount);
        
        // 1. Create In-App notification
        inAppNotificationService.sendDailySummaryNotification(user, appointmentCount);

        // 2. Dispatch Expo push notification if token exists
        expoPushNotificationService.sendDailySummaryNotification(user, appointmentCount);
    }

    @Override
    public void sendNotification(User user, String title, String message) {
        if (user == null) {
            log.warn("Cannot dispatch notification: User is null");
            return;
        }

        log.info("Dispatching notification for user ID: {}", user.getId());
        
        // 1. Create In-App notification
        inAppNotificationService.sendNotification(user, title, message);

        // 2. Dispatch Expo push notification
        expoPushNotificationService.sendNotification(user, title, message);
    }
}
