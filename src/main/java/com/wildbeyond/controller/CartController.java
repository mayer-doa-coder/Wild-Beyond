package com.wildbeyond.controller;

import com.wildbeyond.service.OrderService;
import com.wildbeyond.service.ProductService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/cart")
@PreAuthorize("hasAnyRole('BUYER', 'SELLER')")
@RequiredArgsConstructor
public class CartController {

    private static final String CART_SESSION_KEY = "CART_ITEMS";

    private final ProductService productService;
    private final OrderService orderService;

    @GetMapping("")
    public String cart(Model model, HttpSession session) {
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
        return "cart";
    }

    @PostMapping("/add/{productId}")
    public String addToCart(@PathVariable Long productId,
                            @RequestParam(value = "quantity", defaultValue = "1") Integer quantity,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (quantity == null || quantity < 1) {
            redirectAttributes.addFlashAttribute("productCreateError", "Quantity must be at least 1.");
            return "redirect:/products/" + productId;
        }

        if (!productService.canCurrentUserBuy(productId)) {
            redirectAttributes.addFlashAttribute("productCreateError", "You are not allowed to buy this product.");
            return "redirect:/products/" + productId;
        }

        Map<Long, Integer> cart = getCart(session);
        Integer existing = cart.getOrDefault(productId, 0);
        cart.put(productId, existing + quantity);
        session.setAttribute(CART_SESSION_KEY, cart);

        redirectAttributes.addFlashAttribute("productCreateSuccess", "Product added to cart.");
        return "redirect:/cart";
    }

    @PostMapping("/remove/{productId}")
    public String removeFromCart(@PathVariable Long productId, HttpSession session) {
        Map<Long, Integer> cart = getCart(session);
        cart.remove(productId);
        session.setAttribute(CART_SESSION_KEY, cart);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(HttpSession session, RedirectAttributes redirectAttributes) {
        Map<Long, Integer> cart = getCart(session);
        if (cart.isEmpty()) {
            redirectAttributes.addFlashAttribute("productCreateError", "Your cart is empty.");
            return "redirect:/cart";
        }

        try {
            orderService.createFromCart(cart);
            session.setAttribute(CART_SESSION_KEY, new LinkedHashMap<Long, Integer>());
            redirectAttributes.addFlashAttribute("productCreateSuccess", "Checkout completed successfully.");
            return "redirect:/orders";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("productCreateError", ex.getMessage());
            return "redirect:/cart";
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

    public record CartLineView(Long productId,
                               String productName,
                               Integer quantity,
                               BigDecimal unitPrice,
                               BigDecimal lineTotal,
                               boolean imageAvailable) {
    }
}
