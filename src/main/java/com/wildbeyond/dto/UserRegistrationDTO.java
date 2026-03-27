package com.wildbeyond.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for user registration input.
 * Used by AuthController and the registration form.
 *
 * Security note: password is annotated WRITE_ONLY — it is accepted on input
 * but will never be serialized back in any JSON response.
 */
@Data
public class UserRegistrationDTO {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    /**
     * Accepted on input only. WRITE_ONLY prevents this field from ever appearing
     * in a serialized response, even if this DTO is accidentally returned from a controller.
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
            message = "Password must include uppercase, lowercase, number, and special character"
        )
    private String password;

    /**
     * Role requested during registration: "BUYER" or "SELLER".
     * "ADMIN" is never accepted here — admin accounts are assigned by existing admins only.
     * @Pattern enforces this at the DTO layer before the request reaches the service.
     */
    @NotBlank(message = "Please select a role")
    @Pattern(regexp = "^(BUYER|SELLER)$", message = "Role must be BUYER or SELLER")
    private String role;
}
