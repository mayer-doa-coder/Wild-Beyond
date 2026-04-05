package com.wildbeyond.controller;

import com.wildbeyond.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Order views (Thymeleaf MVC).
 * Requires authentication — any logged-in user (BUYER, SELLER, ADMIN).
 *
 * REST operations live in controller/rest/OrderRestController.
 */
@Controller
@RequestMapping({"/orders", "/admin/orders", "/seller/orders", "/buyer/orders"})
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("")
    public String getOrders(@RequestParam(value = "view", required = false) String view,
                            Model model,
                            Authentication authentication,
                            HttpServletRequest request) {
        String basePath = resolveOrdersBasePath(request);
        if (!"/orders".equals(basePath)) {
            assertRoleScopedAccess(basePath, authentication);
        }

        String canonicalBasePath = resolveCanonicalOrdersBasePath(authentication);
        if ("/orders".equals(basePath) && isAuthenticated(authentication) && !"/orders".equals(canonicalBasePath)) {
            String viewQuery = (view == null || view.isBlank()) ? "" : "?view=" + view;
            return "redirect:" + canonicalBasePath + viewQuery;
        }

        String effectiveBasePath = "/orders".equals(basePath) ? canonicalBasePath : basePath;
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isSeller = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SELLER"));

        model.addAttribute("ordersBasePath", effectiveBasePath);
        model.addAttribute("productsBasePath", resolveCanonicalProductsBasePath(authentication));
        model.addAttribute("cartBasePath", resolveCanonicalCartBasePath(authentication));

        if (isAdmin) {
            model.addAttribute("orders", orderService.findAll());
            model.addAttribute("ordersView", "all");
            return "orders";
        }

        if (isSeller && "selling".equalsIgnoreCase(view)) {
            model.addAttribute("orders", orderService.findOrdersForCurrentSellerProducts());
            model.addAttribute("ordersView", "selling");
            return "orders";
        }

        model.addAttribute("orders", orderService.findMyOrders());
        model.addAttribute("ordersView", "buying");
        return "orders";
    }

    @GetMapping("/{id}")
    public String getOrderById(@PathVariable Long id,
                               Model model,
                               Authentication authentication,
                               HttpServletRequest request) {
        String basePath = resolveOrdersBasePath(request);
        if (!"/orders".equals(basePath)) {
            assertRoleScopedAccess(basePath, authentication);
        }

        String canonicalBasePath = resolveCanonicalOrdersBasePath(authentication);
        if ("/orders".equals(basePath) && isAuthenticated(authentication) && !"/orders".equals(canonicalBasePath)) {
            return "redirect:" + canonicalBasePath + "/" + id;
        }

        String effectiveBasePath = "/orders".equals(basePath) ? canonicalBasePath : basePath;

        try {
            model.addAttribute("order", orderService.getOrderById(id));
            var allowedStatuses = orderService.getAllowedStatusUpdates(id);
            if (allowedStatuses == null) {
                allowedStatuses = List.of();
            }
            model.addAttribute("allowedStatuses", allowedStatuses);
            model.addAttribute("canUpdateStatus", !allowedStatuses.isEmpty());
            model.addAttribute("ordersBasePath", effectiveBasePath);
            model.addAttribute("productsBasePath", resolveCanonicalProductsBasePath(authentication));
            model.addAttribute("cartBasePath", resolveCanonicalCartBasePath(authentication));
            return "order-detail";
        } catch (AccessDeniedException ex) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ex.getMessage());
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage());
        }
    }

    @PostMapping("/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam("status") String status,
                                    RedirectAttributes redirectAttributes,
                                    Authentication authentication,
                                    HttpServletRequest request) {
        String basePath = resolveOrdersBasePath(request);
        if (!"/orders".equals(basePath)) {
            assertRoleScopedAccess(basePath, authentication);
        }
        String redirectBasePath = "/orders".equals(basePath) ? resolveCanonicalOrdersBasePath(authentication) : basePath;

        try {
            orderService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("orderUpdateSuccess", "Order status updated.");
        } catch (AccessDeniedException ex) {
            redirectAttributes.addFlashAttribute("orderUpdateError", ex.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("orderUpdateError", ex.getMessage());
        }
        return "redirect:" + redirectBasePath + "/" + id;
    }

    @PostMapping("")
    public String createOrder(Authentication authentication,
                              HttpServletRequest request) {
        String basePath = resolveOrdersBasePath(request);
        if (!"/orders".equals(basePath)) {
            assertRoleScopedAccess(basePath, authentication);
        }
        String redirectBasePath = "/orders".equals(basePath) ? resolveCanonicalOrdersBasePath(authentication) : basePath;
        return "redirect:" + redirectBasePath;
    }

    private String resolveOrdersBasePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return "/orders";
        }

        if (uri.startsWith("/admin/orders")) {
            return "/admin/orders";
        }
        if (uri.startsWith("/seller/orders")) {
            return "/seller/orders";
        }
        if (uri.startsWith("/buyer/orders")) {
            return "/buyer/orders";
        }
        return "/orders";
    }

    private String resolveCanonicalOrdersBasePath(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "/orders";
        }

        if (hasRole(authentication, "ROLE_ADMIN")) {
            return "/admin/orders";
        }
        if (hasRole(authentication, "ROLE_SELLER")) {
            return "/seller/orders";
        }
        if (hasRole(authentication, "ROLE_BUYER")) {
            return "/buyer/orders";
        }
        return "/orders";
    }

    private String resolveCanonicalProductsBasePath(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "/products";
        }

        if (hasRole(authentication, "ROLE_ADMIN")) {
            return "/admin/products";
        }
        if (hasRole(authentication, "ROLE_SELLER")) {
            return "/seller/products";
        }
        if (hasRole(authentication, "ROLE_BUYER")) {
            return "/buyer/products";
        }
        return "/products";
    }

    private String resolveCanonicalCartBasePath(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "/cart";
        }

        if (hasRole(authentication, "ROLE_SELLER")) {
            return "/seller/cart";
        }
        if (hasRole(authentication, "ROLE_BUYER")) {
            return "/buyer/cart";
        }
        return "/cart";
    }

    private void assertRoleScopedAccess(String basePath, Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            throw new ResponseStatusException(FORBIDDEN, "Authentication is required");
        }

        if ("/admin/orders".equals(basePath) && !hasRole(authentication, "ROLE_ADMIN")) {
            throw new ResponseStatusException(FORBIDDEN, "Admin role required");
        }
        if ("/seller/orders".equals(basePath) && !hasRole(authentication, "ROLE_SELLER")) {
            throw new ResponseStatusException(FORBIDDEN, "Seller role required");
        }
        if ("/buyer/orders".equals(basePath) && !hasRole(authentication, "ROLE_BUYER")) {
            throw new ResponseStatusException(FORBIDDEN, "Buyer role required");
        }
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !hasRole(authentication, "ROLE_ANONYMOUS");
    }

    private boolean hasRole(Authentication authentication, String roleName) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> roleName.equals(a.getAuthority()));
    }
}