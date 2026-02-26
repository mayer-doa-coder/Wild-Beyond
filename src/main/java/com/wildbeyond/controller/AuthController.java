package com.wildbeyond.controller;

import com.wildbeyond.dto.UserRegistrationDTO;
import com.wildbeyond.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Day-3: AuthController — serves Thymeleaf views for login and registration.
 *
 * NOTE: This controller is a @Controller (NOT @RestController) because it
 * returns view names resolved by Thymeleaf, not JSON bodies.
 *
 * Login flow:
 *   GET  /auth/login  → render login.html
 *   POST /auth/login  → handled entirely by Spring Security (NOT this controller).
 *                       SecurityConfig.formLogin() intercepts the POST.
 *
 * Register flow (stub — service wired on Day-4):
 *   GET  /auth/register  → render register.html with empty form DTO
 *   POST /auth/register  → will delegate to AuthService.register() on Day-4
 *
 * Logout:
 *   POST /auth/logout → handled entirely by Spring Security.
 *                       SecurityConfig.logout() clears session and redirects.
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * Show the login page.
     *
     * @param error   present when Spring Security redirects here after a failed login
     * @param logout  present when Spring Security redirects here after a successful logout
     * @param model   used to pass flags so the template can display feedback messages
     */
    @GetMapping("/login")
    public String showLoginPage(
            @RequestParam(value = "error",  required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            // Invalid email or password — do NOT reveal which one (security best practice)
            model.addAttribute("errorMessage", "Invalid email or password. Please try again.");
        }

        if (logout != null) {
            model.addAttribute("logoutMessage", "You have been logged out successfully.");
        }

        return "login";   // resolves to templates/login.html
    }

    // ── Register ──────────────────────────────────────────────────────────────

    /**
     * Show the registration page with a blank form DTO pre-bound to the form.
     * The DTO is named "registerForm" — Thymeleaf binds it with th:object="${registerForm}".
     */
    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("registerForm", new UserRegistrationDTO());
        return "register";   // resolves to templates/register.html
    }

    /**
     * Handle registration form submission.
     * Validates input, delegates to UserService (BCrypt encoding), then redirects to login.
     */
    @PostMapping("/register")
    public String handleRegister(
            @Valid @ModelAttribute("registerForm") UserRegistrationDTO dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "register";   // re-render form with validation errors
        }

        try {
            userService.registerUser(dto);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Account created successfully! Please sign in.");
            return "redirect:/auth/login";
        } catch (RuntimeException ex) {
            // e.g. duplicate email or role not found
            model.addAttribute("errorMessage", ex.getMessage());
            return "register";
        }
    }
}
