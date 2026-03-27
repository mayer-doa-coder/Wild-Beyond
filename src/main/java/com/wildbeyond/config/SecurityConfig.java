package com.wildbeyond.config;

import com.wildbeyond.service.CustomUserDetailsService;
import com.wildbeyond.security.HttpsEnforcementFilter;
import com.wildbeyond.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Day-3: Full Security Configuration for Wild-Beyond.
 *
 * What this class does:
 *  1. Removes the default generated Spring Security password entirely.
 *  2. Replaces the default login page with our own Thymeleaf login page.
 *  3. Wires our CustomUserDetailsService so login uses EMAIL as identifier.
 *  4. Registers BCryptPasswordEncoder as the global PasswordEncoder bean.
 *  5. Defines URL-level access rules per role (ADMIN / SELLER / BUYER).
 *  6. Configures logout behaviour.
 *
 * Role strategy:
 *   ADMIN  → full platform access, user management, all products & orders
 *   SELLER → manage own products, view own orders
 *   BUYER  → browse products, place & view own orders
 *
 * URL restriction structure:
 *   PUBLIC  (no login required):
 *     GET  /                   — landing page
 *     GET  /products           — product listing (browse)
 *     GET  /products/{id}      — product detail
 *     GET  /auth/login         — login page
 *     GET  /auth/register      — registration page
 *     POST /auth/register      — submit registration
 *
 *   BUYER + SELLER + ADMIN (any authenticated user):
 *     GET  /orders             — own order history
 *     POST /orders             — place a new order
 *
 *   SELLER + ADMIN only:
 *     POST   /products         — create a product
 *     PUT    /products/{id}    — update a product
 *     DELETE /products/{id}    — delete a product
 *
 *   ADMIN only:
 *     /admin/**                — admin dashboard & user management
 *     /users/**                — user CRUD
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // allows @PreAuthorize / @Secured on service/controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final HttpsEnforcementFilter httpsEnforcementFilter;
    private final RateLimitingFilter rateLimitingFilter;

    // ── Password Encoder ─────────────────────────────────────────────────────

    /**
     * BCrypt is the industry standard for hashing passwords at rest.
     * Default strength = 10 (approx. 100 ms per hash — balanced for security vs UX).
     * This bean is injected wherever passwords need to be encoded or verified.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── Authentication Provider ───────────────────────────────────────────────

    /**
     * DaoAuthenticationProvider wires together:
     *   - our CustomUserDetailsService (loads user by email from DB)
     *   - BCryptPasswordEncoder (verifies the submitted password)
     *
     * Spring Security will call loadUserByUsername(email) on every login attempt.
     *
     * NOT a @Bean — registered directly into the filter chain via
     * .authenticationProvider() below. Exposing it as a @Bean while
     * CustomUserDetailsService is also a @Service bean causes Spring Security 7
     * to detect two authentication paths and emit a duplicate-provider warning.
     */
    private DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // ── Authentication Manager ────────────────────────────────────────────────

    /**
     * Exposes the AuthenticationManager as a bean so it can be injected into
     * AuthService (needed for programmatic login during registration flow).
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    // ── Security Filter Chain ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Wire our DaoAuthenticationProvider explicitly — avoids Spring Security 7
            // warning about UserDetailsService bean + AuthenticationProvider bean coexisting.
            .authenticationProvider(authenticationProvider())

            // ── HTTP Basic Auth (REST API clients) ────────────────────────────
            // Enables Authorization: Basic <base64> on /api/** requests.
            // Allows Thunder Client / Postman / curl to authenticate without a browser session.
            // Form login still handles browser-based authentication normally.
            .httpBasic(basic -> {})

            .addFilterBefore(httpsEnforcementFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)

            // ── URL Access Rules ─────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // Static resources — always public
                .requestMatchers(
                    "/css/**", "/js/**", "/images/**", "/webjars/**"
                ).permitAll()

                // Spring Boot default error endpoint — must be public so error messages
                // are returned correctly instead of redirecting to login (401).
                .requestMatchers("/error").permitAll()

                // Public pages — no login required
                .requestMatchers(
                    "/",
                    "/auth/login",
                    "/auth/register",
                    "/actuator/health"
                ).permitAll()

                // Product browsing — public (GET only)
                .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()

                // Product write rules:
                //   POST   → SELLER only  (only sellers list products)
                //   PUT    → SELLER + ADMIN  (admin can correct any listing)
                //   DELETE → SELLER + ADMIN  (admin can remove harmful content)
                .requestMatchers(HttpMethod.POST,   "/products").hasRole("SELLER")
                .requestMatchers(HttpMethod.PUT,    "/products/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/products/**").hasAnyRole("SELLER", "ADMIN")

                // REST API — /api/products
                .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.POST,   "/api/products").hasRole("SELLER")
                .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("SELLER", "ADMIN")

                // REST API — /api/orders
                //   POST   /api/orders        — BUYER only (sellers sell, buyers buy)
                //   GET    /api/orders        — ADMIN only (full platform view)
                //   GET    /api/orders/my     — any authenticated user sees their own orders
                //   GET    /api/orders/{id}   — any authenticated user
                //   DELETE /api/orders/{id}   — ADMIN only
                //
                // IMPORTANT: Specific paths must come before wildcards.
                // Spring's AntPathMatcher matches /api/orders/** against /api/orders
                // (the ** wildcard includes zero segments), so if the wildcard rule
                // appears first it shadows the exact-path rule below it.
                .requestMatchers(HttpMethod.POST,   "/api/orders").hasRole("BUYER")
                .requestMatchers(HttpMethod.GET,    "/api/orders").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/orders/my").authenticated()
                .requestMatchers(HttpMethod.GET,    "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole("ADMIN")

                // Admin only — full user management and admin dashboard
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/seller/**").hasRole("SELLER")
                .requestMatchers("/users/**").hasRole("ADMIN")

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // ── Form Login ────────────────────────────────────────────────────
            // Spring Security default login page is DISABLED.
            // We serve our own Thymeleaf login page.
            // The "username" parameter on the form MUST be named "email"
            // so that Spring Security passes the email value to
            // CustomUserDetailsService.loadUserByUsername().
            .formLogin(form -> form
                .loginPage("/auth/login")                 // GET — show login form
                .loginProcessingUrl("/auth/login")        // POST — Spring processes credentials
                .usernameParameter("email")               // form field name (not "username")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)    // redirect to role-based dashboard
                .failureUrl("/auth/login?error=true")     // redirect on bad credentials
                .permitAll()
            )

            // ── Logout ────────────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/auth/logout")                // POST /auth/logout — Spring handles this
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            // ── CSRF ──────────────────────────────────────────────────────────
            // CSRF protection is ENABLED for all Thymeleaf form-based routes.
            // Thymeleaf automatically injects the _csrf token into all forms.
            //
            // REST API endpoints (/api/**) are exempt:
            //   - They are consumed by API clients (Postman, fetch, etc.) that
            //     do not carry the session cookie used for CSRF validation.
            //   - Authorization is still enforced via Spring Security roles.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")
            );

        http.headers(headers -> headers
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'; form-action 'self'"))
                .frameOptions(frame -> frame.deny())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .maxAgeInSeconds(31536000)
                )
        );

        return http.build();
    }
}
