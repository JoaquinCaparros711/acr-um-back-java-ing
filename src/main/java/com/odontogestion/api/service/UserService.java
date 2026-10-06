package com.odontogestion.api.service;

import com.odontogestion.api.entity.User;
import com.odontogestion.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public void updatePushToken(User authenticatedUser, String pushToken) {
        if (authenticatedUser == null) {
            throw new IllegalArgumentException("Authenticated user must not be null");
        }

        User user = userRepository.findById(authenticatedUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + authenticatedUser.getId()));

        log.info("Updating push token for user ID: {}", user.getId());
        user.setPushToken(pushToken);
        userRepository.save(user);
    }
}
