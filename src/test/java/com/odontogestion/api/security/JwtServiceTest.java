package com.odontogestion.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService Security & Parsing Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;

    // Secure 256-bit test key encoded in Base64
    private static final String BASE64_SECRET =
            "NDM1MjUzNzk4OTY2NTQzMjExMjM0NTY3ODkwMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", BASE64_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour
    }

    @Test
    @DisplayName("Should generate and extract subject successfully")
    void should_GenerateTokenAndExtractUsername_When_UserDetailsProvided() {
        // Arrange
        UserDetails userDetails = new User("doctor_smith", "password123", Collections.emptyList());

        // Act
        String token = jwtService.generateToken(userDetails);
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertThat(token).isNotBlank();
        assertThat(extractedUsername).isEqualTo("doctor_smith");
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("Should fail validation when username does not match")
    void should_ReturnFalse_When_TokenUsernameDoesNotMatch() {
        // Arrange
        UserDetails userDetails = new User("doctor_smith", "password123", Collections.emptyList());
        UserDetails differentUser = new User("dr_jones", "password456", Collections.emptyList());

        // Act
        String token = jwtService.generateToken(userDetails);

        // Assert
        assertThat(jwtService.isTokenValid(token, differentUser)).isFalse();
    }
}
