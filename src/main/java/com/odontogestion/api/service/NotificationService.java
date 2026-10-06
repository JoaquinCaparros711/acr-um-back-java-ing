package com.odontogestion.api.service;

import com.odontogestion.api.entity.User;

/**
 * Interface defining notification dispatch capabilities.
 */
public interface NotificationService {

    /**
     * Dispatches a daily appointment summary notification to the target user.
     *
     * @param user             The recipient dentist.
     * @param appointmentCount The number of scheduled appointments for today.
     */
    void sendDailySummaryNotification(User user, int appointmentCount);

    /**
     * Dispatches a generic notification to the target user.
     *
     * @param user    The recipient user.
     * @param title   Notification title.
     * @param message Notification body message.
     */
    void sendNotification(User user, String title, String message);
}
