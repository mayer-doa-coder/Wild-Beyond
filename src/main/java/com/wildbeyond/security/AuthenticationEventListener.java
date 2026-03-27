package com.wildbeyond.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationEventListener {

    private final LoginAttemptService loginAttemptService;

    @EventListener
    public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
        String email = event.getAuthentication().getName();
        if (email == null || email.isBlank()) {
            return;
        }
        loginAttemptService.recordFailedAttempt(email);
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        if (email == null || email.isBlank()) {
            return;
        }
        loginAttemptService.recordSuccessfulLogin(email);
    }
}
