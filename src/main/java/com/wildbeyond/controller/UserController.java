package com.wildbeyond.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * User management — ADMIN only.
 * Enforced at two layers:
 *   1. URL-level via SecurityConfig  (/users/** → hasRole ADMIN)
 *   2. Method-level via @PreAuthorize (defense-in-depth)
 */
@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    @GetMapping("")
    public String getAllUsers() {
        return "redirect:/dashboard";
    }

    @GetMapping("/{id}")
    public String getUserById(@PathVariable Long id) {
        return "redirect:/users";
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable Long id) {
        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        return "redirect:/users";
    }
}