package com.wildbeyond.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * User management (ADMIN only — enforced by SecurityConfig).
 * Stub — redirects to dashboard until user management views are built.
 */
@Controller
@RequestMapping("/users")
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