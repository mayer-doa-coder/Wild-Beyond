package com.wildbeyond.controller;

import com.wildbeyond.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping({"/users", "/admin/users"})
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("")
    public String getAllUsers(Model model, HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/users")) {
            return "redirect:/admin/users";
        }

        model.addAttribute("users", userRepository.findAllByOrderByIdAsc());
        return "users";
    }

    @GetMapping("/{id}")
    public String getUserById(@PathVariable Long id) {
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/edit")
    public String updateUser(@PathVariable Long id) {
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id) {
        return "redirect:/admin/users";
    }
}