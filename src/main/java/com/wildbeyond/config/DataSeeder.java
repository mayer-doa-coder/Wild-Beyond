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
 *   ADMIN_EMAIL / APP_ADMIN_EMAIL    (default: admin@wildbeyond.com)
 *   ADMIN_PASSWORD / APP_ADMIN_PASSWORD (default: Admin@123)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:${APP_ADMIN_EMAIL:${app.admin.email:admin@wildbeyond.com}}}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:${APP_ADMIN_PASSWORD:${app.admin.password:Admin@123}}}")
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
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new RuntimeException("ADMIN role not found after seeding"));

        User admin = userRepository.findByEmail(adminEmail)
            .map(existing -> {
                existing.setEnabled(true);
                existing.setPassword(passwordEncoder.encode(adminPassword));

                    existing.setRoles(Set.of(adminRole));

                return existing;
            })
            .orElseGet(() -> User.builder()
                .name("Admin")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .enabled(true)
                .roles(Set.of(adminRole))
                .build());

        userRepository.save(admin);
        log.info("DataSeeder: ensured default admin user '{}'", adminEmail);
    }
}
