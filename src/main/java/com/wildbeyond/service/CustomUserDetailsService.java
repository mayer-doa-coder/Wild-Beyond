package com.wildbeyond.service;

import com.wildbeyond.model.User;
import com.wildbeyond.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Day-3: Custom UserDetailsService implementation.
 *
 * Spring Security calls loadUserByUsername() during authentication.
 * In Wild-Beyond we use EMAIL as the login identifier — not a username string.
 * The method parameter is named "username" by the interface contract but we
 * treat it as an email address throughout.
 *
 * Role strategy:
 *   ADMIN  — platform administrator
 *   SELLER — can list and manage products
 *   BUYER  — can browse products and place orders
 *
 * Spring Security requires authority strings in the format "ROLE_XXX".
 * Our Role entity stores plain names (ADMIN, SELLER, BUYER), so we prefix
 * each with "ROLE_" here when building the GrantedAuthority set.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load a user by their email address.
     *
     * @param email the email submitted on the login form (mapped to the
     *              "username" field by Spring Security's contract)
     * @return a fully populated UserDetails object
     * @throws UsernameNotFoundException if no account exists for that email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("No account found for email: " + email));

        // Map each Role name → "ROLE_ADMIN" / "ROLE_SELLER" / "ROLE_BUYER"
        Set<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())   // email IS the principal name
                .password(user.getPassword())    // BCrypt-encoded hash
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isEnabled())
                .build();
    }
}
