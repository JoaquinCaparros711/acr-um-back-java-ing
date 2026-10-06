package com.odontogestion.api.service;

import com.odontogestion.api.dto.ExpoPushMessageDTO;
import com.odontogestion.api.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExpoPushNotificationService implements NotificationService {

    private final RestTemplate restTemplate;

    @Value("${expo.push-notification.url:https://exp.host/--/api/v2/push/send}")
    private String expoPushUrl;

    @Override
    public void sendDailySummaryNotification(User user, int appointmentCount) {
        if (user == null || user.getPushToken() == null || user.getPushToken().isBlank()) {
            log.warn("Skipping Expo push notification: User or push token is missing for user ID: {}",
                    user != null ? user.getId() : "null");
            return;
        }

        String title = "Agenda del Día";
        String message = appointmentCount == 1
                ? "Tienes 1 turno programado para hoy."
                : "Tienes " + appointmentCount + " turnos programados para hoy.";

        sendPushNotification(user.getPushToken(), title, message, Map.of("screen", "appointments", "count", appointmentCount));
    }

    @Override
    public void sendNotification(User user, String title, String message) {
        if (user == null || user.getPushToken() == null || user.getPushToken().isBlank()) {
            log.warn("Skipping Expo push notification: User or push token is missing");
            return;
        }

        sendPushNotification(user.getPushToken(), title, message, Map.of());
    }

    public void sendPushNotification(String pushToken, String title, String message, Map<String, Object> data) {
        if (pushToken == null || pushToken.isBlank()) {
            return;
        }

        try {
            ExpoPushMessageDTO pushPayload = ExpoPushMessageDTO.builder()
                    .to(pushToken)
                    .title(title)
                    .body(message)
                    .data(data)
                    .sound("default")
                    .priority("high")
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ExpoPushMessageDTO> request = new HttpEntity<>(pushPayload, headers);

            log.info("Sending Expo push notification to token: {}", pushToken);
            restTemplate.postForEntity(expoPushUrl, request, String.class);
            log.info("Expo push notification sent successfully");
        } catch (Exception e) {
            log.error("Failed to send Expo push notification to token {}: {}", pushToken, e.getMessage(), e);
        }
    }
}
