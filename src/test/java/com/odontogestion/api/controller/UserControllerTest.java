package com.odontogestion.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odontogestion.api.dto.PushTokenRequestDTO;
import com.odontogestion.api.entity.Role;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.exception.GlobalExceptionHandler;
import com.odontogestion.api.security.AuthenticationFacade;
import com.odontogestion.api.service.UserService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private AuthenticationFacade authenticationFacade;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();

        mockUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@test.com")
                .role(Role.DENTIST)
                .build();
    }

    @Test
    @DisplayName("should_UpdatePushToken_When_ValidRequestProvided")
    void should_UpdatePushToken_When_ValidRequestProvided() throws Exception {
        // Arrange
        PushTokenRequestDTO requestDTO = PushTokenRequestDTO.builder()
                .pushToken("ExponentPushToken[xyz123]")
                .build();

        when(authenticationFacade.getAuthenticatedUser()).thenReturn(mockUser);

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/push-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Push token registered successfully"));

        verify(userService).updatePushToken(eq(mockUser), eq("ExponentPushToken[xyz123]"));
    }

    @Test
    @DisplayName("should_ReturnBadRequest_When_PushTokenIsBlank")
    void should_ReturnBadRequest_When_PushTokenIsBlank() throws Exception {
        // Arrange
        PushTokenRequestDTO requestDTO = PushTokenRequestDTO.builder()
                .pushToken("")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/users/push-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isBadRequest());
    }
}
