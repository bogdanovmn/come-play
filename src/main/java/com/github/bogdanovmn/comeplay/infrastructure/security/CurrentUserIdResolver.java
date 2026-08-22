package com.github.bogdanovmn.comeplay.infrastructure.security;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("currentUserIdResolver")
class CurrentUserIdResolver {
    UUID resolve(Object principal) {
        if (principal instanceof JwtBasedUserDetails user) {
            return user.getUserId();
        }
        return null;
    }
}
