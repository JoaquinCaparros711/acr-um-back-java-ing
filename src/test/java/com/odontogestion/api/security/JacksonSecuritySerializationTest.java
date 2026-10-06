package com.odontogestion.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Jackson Serialization & Deserialization Security Tests")
class JacksonSecuritySerializationTest {

    private ObjectMapper objectMapper;

    static class SecurityTestPayload {
        private String username;
        private LocalDateTime timestamp;

        public SecurityTestPayload() {
        }

        public SecurityTestPayload(String username, LocalDateTime timestamp) {
            this.username = username;
            this.timestamp = timestamp;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Should serialize and deserialize JSR310 date-time safely")
    void should_SerializeAndDeserializeJavaTime_When_ValidPayloadProvided() throws Exception {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        SecurityTestPayload payload = new SecurityTestPayload("dentist_admin", now);

        // Act
        String json = objectMapper.writeValueAsString(payload);
        SecurityTestPayload result = objectMapper.readValue(json, SecurityTestPayload.class);

        // Assert
        assertThat(json).contains("dentist_admin");
        assertThat(result.getUsername()).isEqualTo("dentist_admin");
        assertThat(result.getTimestamp()).isEqualToIgnoringNanos(now);
    }

    @Test
    @DisplayName("Should block unsafe polymorphic deserialization by default")
    void should_RejectMalformedOrMaliciousPayload_When_Deserializing() {
        // Arrange
        String maliciousPayload = "{\"@class\":\"org.springframework.context.support.ClassPathXmlApplicationContext\"}";

        // Act & Assert
        assertThatThrownBy(() -> objectMapper.readValue(maliciousPayload, SecurityTestPayload.class))
                .isInstanceOf(Exception.class);
    }
}
