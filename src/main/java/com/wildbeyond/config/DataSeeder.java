package com.wildbeyond.config;

import com.wildbeyond.model.Role;
import com.wildbeyond.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ensures the roles table is pre-populated with the three base roles
 * (BUYER, SELLER, ADMIN) on every application startup.
 *
 * Uses "insert if absent" logic — safe to run repeatedly without duplicates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        List<String> requiredRoles = List.of("BUYER", "SELLER", "ADMIN");

        for (String roleName : requiredRoles) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(new Role(null, roleName));
                log.info("DataSeeder: inserted role '{}'", roleName);
            } else {
                log.debug("DataSeeder: role '{}' already exists — skipped", roleName);
            }
        }
    }
}
