package com.wildbeyond.controller;

import com.wildbeyond.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("")
    public String getAllUsers(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "users";
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