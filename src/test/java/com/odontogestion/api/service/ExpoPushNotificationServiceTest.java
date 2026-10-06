package com.odontogestion.api.service;

import com.odontogestion.api.entity.Role;
import com.odontogestion.api.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpoPushNotificationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExpoPushNotificationService expoPushNotificationService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@test.com")
                .role(Role.DENTIST)
                .pushToken("ExpoPushToken[abcdef123456]")
                .build();

        ReflectionTestUtils.setField(expoPushNotificationService, "expoPushUrl", "https://exp.host/--/api/v2/push/send");
    }

    @Test
    @DisplayName("should_PostToExpoPushApi_When_ValidUserAndPushTokenProvided")
    void should_PostToExpoPushApi_When_ValidUserAndPushTokenProvided() {
        // Arrange
        when(restTemplate.postForEntity(eq("https://exp.host/--/api/v2/push/send"), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"data\":{\"status\":\"ok\"}}"));

        // Act
        expoPushNotificationService.sendDailySummaryNotification(mockUser, 3);

        // Assert
        verify(restTemplate).postForEntity(eq("https://exp.host/--/api/v2/push/send"), any(HttpEntity.class), eq(String.class));
    }

    @Test
    @DisplayName("should_NotPostToApi_When_PushTokenIsMissingOrEmpty")
    void should_NotPostToApi_When_PushTokenIsMissingOrEmpty() {
        // Arrange
        mockUser.setPushToken(null);

        // Act
        expoPushNotificationService.sendDailySummaryNotification(mockUser, 2);

        // Assert
        verify(restTemplate, never()).postForEntity(any(), any(), any());
    }
}
