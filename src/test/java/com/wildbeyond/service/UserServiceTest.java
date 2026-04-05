package com.wildbeyond.service;

import com.wildbeyond.dto.UserRegistrationDTO;
import com.wildbeyond.model.Role;
import com.wildbeyond.model.User;
import com.wildbeyond.repository.RoleRepository;
import com.wildbeyond.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 *
 * Uses Mockito only — no Spring context or database required.
 * Covers: registerUser (happy path, duplicate email, missing role).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRegistrationDTO dto;
    private Role buyerRole;

    @BeforeEach
    void setUp() {
        dto = new UserRegistrationDTO();
        dto.setName("Alice");
        dto.setEmail("alice@example.com");
        dto.setPassword("securePass1");
        dto.setRole("BUYER");

        buyerRole = new Role();
        buyerRole.setId(1L);
        buyerRole.setName("BUYER");
    }

    // ── registerUser ─────────────────────────────────────────────────────────

    @Test
    void registerUser_savesUser_withHashedPassword() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByName("BUYER")).thenReturn(Optional.of(buyerRole));
        when(passwordEncoder.encode("securePass1")).thenReturn("$2a$10$hashedValue");

        userService.registerUser(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        // Raw password must NOT be stored — only the BCrypt hash
        assertThat(saved.getPassword()).isEqualTo("$2a$10$hashedValue");
        assertThat(saved.getPassword()).doesNotContain("securePass1");
        assertThat(saved.getRoles()).contains(buyerRole);
    }

    @Test
    void registerUser_assignsCorrectRole_toBuyer() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("BUYER")).thenReturn(Optional.of(buyerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        userService.registerUser(dto);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRoles()).extracting(Role::getName).containsExactly("BUYER");
    }

    @Test
    void registerUser_throwsException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("email already exists");

        // Must not touch role lookup or persistence
        verify(roleRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_throwsException_whenRoleNotFound() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(roleRepository.findByName("BUYER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("BUYER");

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_encodesPassword_beforePersisting() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName("BUYER")).thenReturn(Optional.of(buyerRole));
        when(passwordEncoder.encode("securePass1")).thenReturn("$2a$10$encodedHash");

        userService.registerUser(dto);

        // PasswordEncoder must always be called — never skip encoding
        verify(passwordEncoder).encode("securePass1");
    }

    @Test
    void countUsers_returnsRepositoryCount() {
        when(userRepository.count()).thenReturn(15L);

        assertThat(userService.countUsers()).isEqualTo(15L);
    }
}
