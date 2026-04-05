package com.wildbeyond.controller;

import com.wildbeyond.service.OrderService;
import com.wildbeyond.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Controller
@RequestMapping({"/cart", "/buyer/cart", "/seller/cart"})
@PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
@RequiredArgsConstructor
public class CartController {

    private static final String CART_SESSION_KEY = "CART_ITEMS";

    private final ProductService productService;
    private final OrderService orderService;

    @GetMapping("")
    public String cart(Model model,
                       HttpSession session,
                       Authentication authentication,
                       HttpServletRequest request) {
        String basePath = resolveCartBasePath(request);
        if (!"/cart".equals(basePath)) {
            assertRoleScopedAccess(basePath, authentication);
        }

        String canonicalBasePath = resolveCanonicalCartBasePath(authentication);
        if ("/cart".equals(basePath) && isAuthenticated(authentication) && !"/cart".equals(canonicalBasePath)) {
            return "redirect:" + canonicalBasePath;
        }

        Map<Long, Integer> cart = getCart(session);
        List<CartLineView> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();
            var product = productService.findById(productId);
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(quantity));
            total = total.add(lineTotal);

            items.add(new CartLineView(
                    product.getId(),
                    product.getName(),
                    quantity,
                    product.getPrice(),
                    lineTotal,
                    product.getImageData() != null && product.getImageData().length > 0
            ));
        }

        model.addAttribute("cartItems", items);
        model.addAttribute("cartTotal", total);
        model.addAttribute("cartBasePath", basePath);
        model.addAttribute("productsBasePath", resolveCanonicalProductsBasePath(authentication));
        return "cart";
    }

    @PostMapping("/add/{productId}")
    public String addToCart(@PathVariable Long productId,
                            @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes,
                            Authentication authentication) {
        String cartBasePath = resolveCanonicalCartBasePath(authentication);
        String productsBasePath = resolveCanonicalProductsBasePath(authentication);

        if (quantity == null || quantity < 1) {
            redirectAttributes.addFlashAttribute("productCreateError", "Quantity must be at least 1.");
            return "redirect:" + productsBasePath + "/" + productId;
        }

        if (!productService.canCurrentUserBuy(productId)) {
            redirectAttributes.addFlashAttribute("productCreateError", "You are not allowed to buy this product.");
            return "redirect:" + productsBasePath + "/" + productId;
        }

        Map<Long, Integer> cart = getCart(session);
        Integer existing = cart.getOrDefault(productId, 0);
        cart.put(productId, existing + quantity);
        session.setAttribute(CART_SESSION_KEY, cart);

        redirectAttributes.addFlashAttribute("productCreateSuccess", "Product added to cart.");
        return "redirect:" + cartBasePath;
    }

    @PostMapping("/remove/{productId}")
    public String removeFromCart(@PathVariable Long productId,
                                 HttpSession session,
                                 Authentication authentication) {
        Map<Long, Integer> cart = getCart(session);
        cart.remove(productId);
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:" + resolveCanonicalCartBasePath(authentication);
    }

    @PostMapping("/checkout")
    public String checkout(HttpSession session,
                           RedirectAttributes redirectAttributes,
                           Authentication authentication) {
        String cartBasePath = resolveCanonicalCartBasePath(authentication);
        String ordersBasePath = resolveCanonicalOrdersBasePath(authentication);

        Map<Long, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("productCreateError", "Your cart is empty.");
            return "redirect:" + cartBasePath;
        }

        try {
            orderService.createFromCart(cart);
            session.setAttribute(CART_SESSION_KEY, new LinkedHashMap<Long, Integer>());
            redirectAttributes.addFlashAttribute("productCreateSuccess", "Checkout completed successfully.");
            return "redirect:" + ordersBasePath;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("productCreateError", ex.getMessage());
            return "redirect:" + cartBasePath;
        }
    }

    private Map<Long, Integer> getCart(HttpSession session) {
        Object existing = session.getAttribute(CART_SESSION_KEY);
        if (existing instanceof Map<?, ?> existingMap) {
            Map<Long, Integer> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : existingMap.entrySet()) {
                if (entry.getKey() instanceof Long key && entry.getValue() instanceof Integer value) {
                    result.put(key, value);
                }
            }
            return result;
        }
        return new LinkedHashMap<>();
    }

    private String resolveCartBasePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return "/cart";
        }

        if (uri.startsWith("/seller/cart")) {
            return "/seller/cart";
        }
        if (uri.startsWith("/buyer/cart")) {
            return "/buyer/cart";
        }
        return "/cart";
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

    private void assertRoleScopedAccess(String basePath, Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            throw new ResponseStatusException(FORBIDDEN, "Authentication is required");
        }

        if ("/seller/cart".equals(basePath) && !hasRole(authentication, "ROLE_SELLER")) {
            throw new ResponseStatusException(FORBIDDEN, "Seller role required");
        }
        if ("/buyer/cart".equals(basePath) && !hasRole(authentication, "ROLE_BUYER")) {
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

    public record CartLineView(Long productId,
                               String productName,
                               Integer quantity,
                               BigDecimal unitPrice,
                               BigDecimal lineTotal,
                               boolean imageAvailable) {
    }
}
