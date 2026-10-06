package com.odontogestion.api.scheduler;

import com.odontogestion.api.entity.AppointmentStatus;
import com.odontogestion.api.entity.Role;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.repository.AppointmentRepository;
import com.odontogestion.api.repository.UserRepository;
import com.odontogestion.api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyAppointmentNotificationScheduler {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notificationService;

    @Value("${notification.daily-summary.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${notification.daily-summary.cron:0 0 8 * * MON-SAT}")
    public void sendDailyAppointmentNotifications() {
        if (!enabled) {
            log.info("Daily appointment notification scheduler is disabled via configuration");
            return;
        }

        log.info("Starting daily appointment notification scheduler task...");

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        List<User> users = userRepository.findAll();

        int notifiedUsersCount = 0;

        for (User user : users) {
            if (user.getRole() != Role.DENTIST && user.getRole() != Role.ODONTOLOGO) {
                continue;
            }

            long count = appointmentRepository.countByUserIdAndStartTimeBetweenAndStatusNot(
                    user.getId(),
                    startOfDay,
                    endOfDay,
                    AppointmentStatus.CANCELLED
            );

            if (count > 0) {
                log.info("Found {} active appointment(s) for dentist ID: {}", count, user.getId());
                notificationService.sendDailySummaryNotification(user, (int) count);
                notifiedUsersCount++;
            }
        }

        log.info("Completed daily appointment notification scheduler task. Total dentists notified: {}", notifiedUsersCount);
    }
}
