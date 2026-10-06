package com.odontogestion.api.security;

import com.odontogestion.api.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Component implementation of AuthenticationFacade accessing SecurityContextHolder.
 */
@Component
public class AuthenticationFacadeImpl implements AuthenticationFacade {

    @Override
    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new SecurityException("User is not authenticated or principal is invalid");
        }
        return (User) authentication.getPrincipal();
    }
}
