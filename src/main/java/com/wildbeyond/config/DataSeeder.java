package com.wildbeyond.config;

import com.wildbeyond.model.Role;
import com.wildbeyond.model.User;
import com.wildbeyond.repository.RoleRepository;
import com.wildbeyond.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Ensures the roles table is pre-populated with the three base roles
 * (BUYER, SELLER, ADMIN) on every application startup, and creates
 * a default admin account if one does not already exist.
 *
 * Uses "insert if absent" logic — safe to run repeatedly without duplicates.
 *
 * Admin credentials are read from environment variables:
 *   APP_ADMIN_EMAIL
 *   APP_ADMIN_PASSWORD
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(ApplicationArguments args) {
        // ── Seed roles ────────────────────────────────────────────────────────
        List<String> requiredRoles = List.of("BUYER", "SELLER", "ADMIN");

        for (String roleName : requiredRoles) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(null, roleName));
                log.info("DataSeeder: inserted role '{}'", roleName);
            } else {
                log.debug("DataSeeder: role '{}' already exists — skipped", roleName);
            }
        }

        // ── Seed admin user ───────────────────────────────────────────────────
        if (userRepository.existsByEmail(adminEmail)) {
            log.debug("DataSeeder: admin user '{}' already exists — skipped", adminEmail);
            return;
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found after seeding"));

        User admin = User.builder()
                .name("Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .enabled(true)
                .roles(Set.of(adminRole))
                .build();

        userRepository.save(admin);
        log.info("DataSeeder: created default admin user '{}'", adminEmail);
    }
}
