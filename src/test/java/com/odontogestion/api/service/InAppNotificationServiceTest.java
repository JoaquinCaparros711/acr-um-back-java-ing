package com.odontogestion.api.service;

import com.odontogestion.api.dto.InAppNotificationResponseDTO;
import com.odontogestion.api.entity.InAppNotification;
import com.odontogestion.api.entity.NotificationType;
import com.odontogestion.api.entity.Role;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.repository.InAppNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InAppNotificationServiceTest {

    @Mock
    private InAppNotificationRepository notificationRepository;

    @InjectMocks
    private InAppNotificationService inAppNotificationService;

    private User mockUser;
    private InAppNotification mockNotification;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .firstName("Jane")
                .lastName("Smith")
                .email("jane@test.com")
                .role(Role.DENTIST)
                .build();

        mockNotification = InAppNotification.builder()
                .id(10L)
                .user(mockUser)
                .title("Agenda del Día")
                .message("Tienes 2 turnos programados para hoy.")
                .type(NotificationType.DAILY_SUMMARY)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("should_SaveNotification_When_SendDailySummaryInvoked")
    void should_SaveNotification_When_SendDailySummaryInvoked() {
        // Arrange
        when(notificationRepository.save(any(InAppNotification.class))).thenReturn(mockNotification);

        // Act
        inAppNotificationService.sendDailySummaryNotification(mockUser, 2);

        // Assert
        verify(notificationRepository).save(any(InAppNotification.class));
    }

    @Test
    @DisplayName("should_ReturnUserNotifications_When_GetUserNotificationsInvoked")
    void should_ReturnUserNotifications_When_GetUserNotificationsInvoked() {
        // Arrange
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(mockNotification));

        // Act
        List<InAppNotificationResponseDTO> result = inAppNotificationService.getUserNotifications(mockUser);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Agenda del Día", result.get(0).getTitle());
        assertFalse(result.get(0).isRead());
    }

    @Test
    @DisplayName("should_MarkNotificationAsRead_When_UserIsOwner")
    void should_MarkNotificationAsRead_When_UserIsOwner() {
        // Arrange
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(mockNotification));
        when(notificationRepository.save(any(InAppNotification.class))).thenReturn(mockNotification);

        // Act
        InAppNotificationResponseDTO result = inAppNotificationService.markAsRead(10L, mockUser);

        // Assert
        assertNotNull(result);
        assertTrue(mockNotification.isRead());
        verify(notificationRepository).save(mockNotification);
    }

    @Test
    @DisplayName("should_ThrowSecurityException_When_UserIsNotOwnerOfNotification")
    void should_ThrowSecurityException_When_UserIsNotOwnerOfNotification() {
        // Arrange
        User anotherUser = User.builder().id(2L).build();
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(mockNotification));

        // Act & Assert
        assertThrows(SecurityException.class, () -> inAppNotificationService.markAsRead(10L, anotherUser));
    }
}
