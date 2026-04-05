package com.wildbeyond.config;

import com.wildbeyond.service.CustomUserDetailsService;
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
import org.springframework.security.web.SecurityFilterChain;

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
 *   BUYER + SELLER (authenticated):
 *     GET  /orders             — own order history
 *     POST /orders             — place a new order
 *
 *   SELLER only:
 *     POST   /products         — create a product
 *
 *   SELLER + ADMIN:
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
                    "/home",
                    "/index",
                    "/blog",
                    "/blog/**",
                    "/explore",
                    "/explore/**",
                    "/about",
                    "/auth/login",
                    "/auth/register"
                ).permitAll()

                // Product management pages (MVC) — seller/admin only
                .requestMatchers("/products/edit/**", "/products/delete/**").hasAnyRole("SELLER", "ADMIN")

                // Cart + checkout (MVC) — buyer + seller can purchase; admin excluded
                .requestMatchers("/cart", "/cart/**", "/buyer/cart", "/buyer/cart/**", "/seller/cart", "/seller/cart/**")
                .hasAnyRole("BUYER", "SELLER")

                // Product browsing — public (GET only)
                .requestMatchers(HttpMethod.GET, "/products", "/products/**").permitAll()

                // Product write rules:
                //   POST   → SELLER only
                //   PUT    → SELLER + ADMIN
                //   DELETE → SELLER + ADMIN
                .requestMatchers(HttpMethod.POST,   "/products").hasRole("SELLER")
                .requestMatchers(HttpMethod.PUT,    "/products/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/products/**").hasAnyRole("SELLER", "ADMIN")

                // Role-scoped MVC product routes
                .requestMatchers("/admin/products", "/admin/products/**").hasRole("ADMIN")
                .requestMatchers("/seller/products", "/seller/products/**").hasRole("SELLER")
                .requestMatchers("/buyer/products", "/buyer/products/**").hasRole("BUYER")

                // Role-scoped MVC order routes
                .requestMatchers("/admin/orders", "/admin/orders/**").hasRole("ADMIN")
                .requestMatchers("/seller/orders", "/seller/orders/**").hasRole("SELLER")
                .requestMatchers("/buyer/orders", "/buyer/orders/**").hasRole("BUYER")

                // Unscoped MVC orders route remains available and redirects to canonical role scope
                .requestMatchers("/orders", "/orders/**").authenticated()

                // REST API — /api/products
                .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.POST,   "/api/products").hasRole("SELLER")
                .requestMatchers(HttpMethod.PUT,    "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("SELLER", "ADMIN")

                // REST API — /api/orders
                //   POST   /api/orders        — BUYER + SELLER (seller cannot buy own products)
                //   GET    /api/orders        — ADMIN only (full platform view)
                //   GET    /api/orders/my     — any authenticated user sees their own orders
                //   GET    /api/orders/{id}   — any authenticated user
                //   DELETE /api/orders/{id}   — ADMIN only
                //
                // IMPORTANT: Specific paths must come before wildcards.
                // Spring's AntPathMatcher matches /api/orders/** against /api/orders
                // (the ** wildcard includes zero segments), so if the wildcard rule
                // appears first it shadows the exact-path rule below it.
                .requestMatchers(HttpMethod.POST,   "/api/orders").hasAnyRole("BUYER", "SELLER")
                .requestMatchers(HttpMethod.PUT,    "/api/orders/**").hasAnyRole("BUYER", "ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/orders").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,    "/api/orders/my").authenticated()
                .requestMatchers(HttpMethod.GET,    "/api/orders/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole("ADMIN")

                // Admin only — full user management and admin dashboard
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/users/**", "/admin/users", "/admin/users/**").hasRole("ADMIN")

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

            .rememberMe(remember -> remember
                .rememberMeParameter("remember-me")
                .tokenValiditySeconds(60 * 60 * 24 * 30)
                .key("wild-beyond-remember-me-key")
                .userDetailsService(customUserDetailsService)
            )

            .sessionManagement(session -> session
                .invalidSessionUrl("/auth/login?expired=true")
            )

            // ── Logout ────────────────────────────────────────────────────────
            .logout(logout -> logout
                .logoutUrl("/auth/logout")                // POST /auth/logout — Spring handles this
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
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
            )
            ;

        return http.build();
    }
}
