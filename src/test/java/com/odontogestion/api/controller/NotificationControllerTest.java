package com.odontogestion.api.controller;

import com.odontogestion.api.dto.InAppNotificationResponseDTO;
import com.odontogestion.api.entity.NotificationType;
import com.odontogestion.api.entity.Role;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.exception.GlobalExceptionHandler;
import com.odontogestion.api.security.AuthenticationFacade;
import com.odontogestion.api.service.InAppNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InAppNotificationService inAppNotificationService;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @InjectMocks
    private NotificationController notificationController;

    private User mockUser;
    private InAppNotificationResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@test.com")
                .role(Role.DENTIST)
                .build();

        responseDTO = InAppNotificationResponseDTO.builder()
                .id(10L)
                .title("Agenda del Día")
                .message("Tienes 3 turnos programados para hoy.")
                .type(NotificationType.DAILY_SUMMARY)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("should_ReturnNotifications_When_GetUserNotificationsInvoked")
    void should_ReturnNotifications_When_GetUserNotificationsInvoked() throws Exception {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockUser);
        when(inAppNotificationService.getUserNotifications(mockUser)).thenReturn(List.of(responseDTO));

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].title").value("Agenda del Día"))
                .andExpect(jsonPath("$[0].isRead").value(false));
    }

    @Test
    @DisplayName("should_ReturnUnreadCount_When_GetUnreadCountInvoked")
    void should_ReturnUnreadCount_When_GetUnreadCountInvoked() throws Exception {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockUser);
        when(inAppNotificationService.getUnreadCount(mockUser)).thenReturn(5L);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5));
    }

    @Test
    @DisplayName("should_MarkNotificationAsRead_When_PatchInvoked")
    void should_MarkNotificationAsRead_When_PatchInvoked() throws Exception {
        // Arrange
        responseDTO.setRead(true);
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockUser);
        when(inAppNotificationService.markAsRead(10L, mockUser)).thenReturn(responseDTO);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/notifications/10/read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    @DisplayName("should_MarkAllNotificationsAsRead_When_PatchReadAllInvoked")
    void should_MarkAllNotificationsAsRead_When_PatchReadAllInvoked() throws Exception {
        // Arrange
        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All notifications marked as read"));

        verify(inAppNotificationService).markAllAsRead(mockUser);
    }
}
