package com.odontogestion.api.security;

import com.odontogestion.api.entity.User;

/**
 * Interface abstraction for security authentication context access.
 * Decouples services from static SecurityContextHolder calls.
 */
public interface AuthenticationFacade {
    /**
     * Retrieves the currently authenticated dentist/user domain entity.
     *
     * @return User object representing the authenticated principal.
     */
    User getAuthenticatedUser();
}
