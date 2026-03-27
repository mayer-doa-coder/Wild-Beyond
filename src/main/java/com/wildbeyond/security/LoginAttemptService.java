package com.wildbeyond.security;

import com.wildbeyond.model.User;
import com.wildbeyond.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserRepository userRepository;

    @Value("${app.security.auth.lockout.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.auth.lockout.lock-duration-minutes:15}")
    private long lockDurationMinutes;

    @Transactional
    public void recordFailedAttempt(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (isLockExpired(user)) {
                unlock(user);
            }

            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);

            if (attempts >= maxFailedAttempts) {
                user.setAccountLocked(true);
                user.setLockUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            }

            userRepository.save(user);
        });
    }

    @Transactional
    public void recordSuccessfulLogin(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.isAccountLocked() || user.getLockUntil() != null) {
                unlock(user);
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public void refreshLockState(User user) {
        if (isLockExpired(user)) {
            unlock(user);
            userRepository.save(user);
        }
    }

    private boolean isLockExpired(User user) {
        return user.isAccountLocked()
                && user.getLockUntil() != null
                && user.getLockUntil().isBefore(LocalDateTime.now());
    }

    private void unlock(User user) {
        user.setAccountLocked(false);
        user.setLockUntil(null);
        user.setFailedLoginAttempts(0);
    }
}
