package com.wildbeyond.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalExceptionHandler.
 *
 * Instantiates the handler directly — no Spring context required.
 * MethodArgumentNotValidException is Mockito-mocked because its constructor
 * requires complex infrastructure objects.
 *
 * Covers all three handler methods:
 *   handleNotFound        → 404
 *   handleValidation      → 400 with per-field messages
 *   handleRuntime         → 400 fallback
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ── handleNotFound ────────────────────────────────────────────────────────

    @Test
    void handleNotFound_returns404_withErrorMessage() {
        ResourceNotFoundException ex =
                new ResourceNotFoundException("Product not found with id: 5");

        ResponseEntity<Map<String, String>> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("error", "Product not found with id: 5");
    }

    @Test
    void handleNotFound_returnsBodyWithSingleErrorKey() {
        ResourceNotFoundException ex =
                new ResourceNotFoundException("Order not found with id: 99");

        Map<String, String> body = handler.handleNotFound(ex).getBody();

        assertThat(body).hasSize(1).containsKey("error");
    }

        // ── handleAccessDenied ───────────────────────────────────────────────────

        @Test
        void handleAccessDenied_returns403_withErrorMessage() {
                AccessDeniedException ex =
                                new AccessDeniedException("You are not allowed to manage this product");

                ResponseEntity<Map<String, String>> response = handler.handleAccessDenied(ex);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                assertThat(response.getBody())
                                .isNotNull()
                                .containsEntry("error", "You are not allowed to manage this product");
        }

    // ── handleValidation ─────────────────────────────────────────────────────

    @Test
    void handleValidation_returns400_withFieldErrors() {
        FieldError nameError =
                new FieldError("productDTO", "name", "Product name is required");
        FieldError priceError =
                new FieldError("productDTO", "price", "Price must be a positive value");

        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(nameError, priceError));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("name", "Product name is required")
                .containsEntry("price", "Price must be a positive value");
    }

    @Test
    void handleValidation_returnsEmptyMap_whenNoFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEmpty();
    }

    // ── handleRuntime ─────────────────────────────────────────────────────────

    @Test
    void handleRuntime_returns400_withErrorMessage() {
        RuntimeException ex =
                new RuntimeException("An account with that email already exists.");

        ResponseEntity<Map<String, String>> response = handler.handleRuntime(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .isNotNull()
                .containsEntry("error", "An account with that email already exists.");
    }

    @Test
    void handleRuntime_returnsBodyWithSingleErrorKey() {
        RuntimeException ex = new RuntimeException("Unexpected failure");

        Map<String, String> body = handler.handleRuntime(ex).getBody();

        assertThat(body).hasSize(1).containsKey("error");
    }
}
