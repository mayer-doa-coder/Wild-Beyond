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

import java.util.Set;

/**
 * Ensures the roles table is pre-populated with the three base roles
 * (BUYER, SELLER, ADMIN) on every application startup.
 *
 * Also ensures:
 * - an environment-configured primary admin account
 * - stable demo/test accounts used by API docs and Postman testing
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

    @Value("${app.demo.password:seller123}")
    private String demoPassword;

    @Override
    public void run(ApplicationArguments args) {
        // ── Seed roles ────────────────────────────────────────────────────────
        Role buyerRole = ensureRole("BUYER");
        Role sellerRole = ensureRole("SELLER");
        Role adminRole = ensureRole("ADMIN");

        // ── Seed primary admin from environment ───────────────────────────────
        ensureUserWithRole("Admin", adminEmail, adminPassword, adminRole);

        // ── Seed stable Postman/demo users for local testing ─────────────────
        ensureUserWithRole("Test Buyer", "buyer@test.com", demoPassword, buyerRole);
        ensureUserWithRole("Test Seller", "seller@test.com", demoPassword, sellerRole);
        ensureUserWithRole("Test Admin", "admin@test.com", demoPassword, adminRole);
    }

    private Role ensureRole(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role saved = roleRepository.save(new Role(null, roleName));
                    log.info("DataSeeder: inserted role '{}'", roleName);
                    return saved;
                });
    }

    private void ensureUserWithRole(String name, String email, String rawPassword, Role role) {
        User user = userRepository.findByEmail(email)
                .map(existing -> {
                    existing.setName(name);
                    existing.setEnabled(true);
                    existing.setPassword(passwordEncoder.encode(rawPassword));
                    existing.setRoles(Set.of(role));
                    return existing;
                })
                .orElseGet(() -> User.builder()
                        .name(name)
                        .email(email)
                        .password(passwordEncoder.encode(rawPassword))
                        .enabled(true)
                        .roles(Set.of(role))
                        .build());

        userRepository.save(user);
        log.info("DataSeeder: ensured user '{}' with role '{}'", email, role.getName());
    }
}
