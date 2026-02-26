package com.wildbeyond.service;

import com.wildbeyond.dto.UserRegistrationDTO;
import com.wildbeyond.model.Role;
import com.wildbeyond.model.User;
import com.wildbeyond.repository.RoleRepository;
import com.wildbeyond.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Handles user registration with BCrypt password encoding.
 *
 * Password security:
 *   - Raw password from the DTO is NEVER stored.
 *   - BCryptPasswordEncoder (strength 10) hashes it before persistence.
 *   - The resulting hash starts with "$2a$10$..." in the database.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user from the registration form DTO.
     *
     * @param dto validated registration data (name, email, password, role)
     * @throws RuntimeException if the email is already taken
     * @throws RuntimeException if the requested role does not exist in the DB
     */
    @Transactional
    public void registerUser(UserRegistrationDTO dto) {

        // Guard: reject duplicate emails before hitting the DB unique constraint
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("An account with that email already exists.");
        }

        // Fetch the Role entity — role names (BUYER, SELLER) must exist in the roles table
        Role role = roleRepository.findByName(dto.getRole())
                .orElseThrow(() -> new RuntimeException(
                        "Role '" + dto.getRole() + "' not found. Contact an administrator."));

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        // BCrypt hash — raw password is discarded immediately after this line
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRoles(Set.of(role));
        // createdAt / updatedAt are set automatically by @PrePersist in User

        userRepository.save(user);
    }
}
