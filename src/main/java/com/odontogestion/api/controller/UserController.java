package com.odontogestion.api.controller;

import com.odontogestion.api.dto.PushTokenRequestDTO;
import com.odontogestion.api.entity.User;
import com.odontogestion.api.security.AuthenticationFacade;
import com.odontogestion.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthenticationFacade authenticationFacade;

    @PostMapping("/push-token")
    public ResponseEntity<Map<String, String>> updatePushToken(@Valid @RequestBody PushTokenRequestDTO requestDTO) {
        User authenticatedUser = authenticationFacade.getAuthenticatedUser();
        log.info("Received request to update push token for user ID: {}", authenticatedUser.getId());

        userService.updatePushToken(authenticatedUser, requestDTO.getPushToken());

        return ResponseEntity.ok(Map.of("message", "Push token registered successfully"));
    }
}
