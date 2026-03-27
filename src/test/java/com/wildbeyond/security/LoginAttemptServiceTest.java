package com.wildbeyond.security;

import com.wildbeyond.model.User;
import com.wildbeyond.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginAttemptService, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(loginAttemptService, "lockDurationMinutes", 15L);

        user = new User();
        user.setId(1L);
        user.setEmail("buyer@example.com");
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);
    }

    @Test
    void recordFailedAttempt_locksAccount_afterMaxFailures() {
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));

        for (int i = 0; i < 5; i++) {
            loginAttemptService.recordFailedAttempt("buyer@example.com");
        }

        assertThat(user.isAccountLocked()).isTrue();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockUntil()).isNotNull();
        verify(userRepository, times(5)).save(user);
    }

    @Test
    void refreshLockState_unlocksExpiredLock() {
        user.setAccountLocked(true);
        user.setFailedLoginAttempts(5);
        user.setLockUntil(LocalDateTime.now().minusMinutes(1));

        loginAttemptService.refreshLockState(user);

        assertThat(user.isAccountLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockUntil()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void recordSuccessfulLogin_clearsFailureState() {
        user.setAccountLocked(true);
        user.setFailedLoginAttempts(3);
        user.setLockUntil(LocalDateTime.now().plusMinutes(5));
        when(userRepository.findByEmail("buyer@example.com")).thenReturn(Optional.of(user));

        loginAttemptService.recordSuccessfulLogin("buyer@example.com");

        assertThat(user.isAccountLocked()).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockUntil()).isNull();
        verify(userRepository).save(user);
    }
}
