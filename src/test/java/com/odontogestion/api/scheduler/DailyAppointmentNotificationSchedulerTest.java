package com.odontogestion.api.scheduler;

import com.odontogestion.api.entity.AppointmentStatus;
import com.odontogestion.api.entity.Role;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.repository.AppointmentRepository;
import com.odontogestion.api.repository.UserRepository;
import com.odontogestion.api.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyAppointmentNotificationSchedulerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DailyAppointmentNotificationScheduler scheduler;

    private User dentistUser;

    @BeforeEach
    void setUp() {
        dentistUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("dentist@test.com")
                .role(Role.DENTIST)
                .pushToken("ExpoPushToken[123456]")
                .build();

        ReflectionTestUtils.setField(scheduler, "enabled", true);
    }

    @Test
    @DisplayName("should_SendDailyNotification_When_AppointmentsExistForDentist")
    void should_SendDailyNotification_When_AppointmentsExistForDentist() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(dentistUser));
        when(appointmentRepository.countByUserIdAndStartTimeBetweenAndStatusNot(
                eq(1L), any(), any(), eq(AppointmentStatus.CANCELLED)
        )).thenReturn(3L);

        // Act
        scheduler.sendDailyAppointmentNotifications();

        // Assert
        verify(notificationService).sendDailySummaryNotification(dentistUser, 3);
    }

    @Test
    @DisplayName("should_NotSendNotification_When_NoAppointmentsScheduledForToday")
    void should_NotSendNotification_When_NoAppointmentsScheduledForToday() {
        // Arrange
        when(userRepository.findAll()).thenReturn(List.of(dentistUser));
        when(appointmentRepository.countByUserIdAndStartTimeBetweenAndStatusNot(
                eq(1L), any(), any(), eq(AppointmentStatus.CANCELLED)
        )).thenReturn(0L);

        // Act
        scheduler.sendDailyAppointmentNotifications();

        // Assert
        verify(notificationService, never()).sendDailySummaryNotification(any(), any(Integer.class));
    }

    @Test
    @DisplayName("should_NotExecuteTask_When_SchedulerDisabledByConfig")
    void should_NotExecuteTask_When_SchedulerDisabledByConfig() {
        // Arrange
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        // Act
        scheduler.sendDailyAppointmentNotifications();

        // Assert
        verify(userRepository, never()).findAll();
        verify(notificationService, never()).sendDailySummaryNotification(any(), any(Integer.class));
    }
}
