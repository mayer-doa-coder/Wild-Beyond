package com.wildbeyond.controller;

import com.wildbeyond.dto.ProductDTO;
import com.wildbeyond.repository.UserRepository;
import com.wildbeyond.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Product views (Thymeleaf MVC).
 * GET endpoints are public (browsing). Write operations require SELLER or ADMIN.
 * URL-level rules are in SecurityConfig; @PreAuthorize adds defense-in-depth.
 *
 * REST operations live in controller/rest/ProductRestController.
 */
@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    @GetMapping("")
    public String getAllProducts(Model model, Authentication authentication) {
        var products = productService.findAll();
        model.addAttribute("products", products);
        ProductDTO newProduct = new ProductDTO();
        newProduct.setSellerId(0L);
        model.addAttribute("newProduct", newProduct);

        Set<Long> manageableProductIds = Collections.emptySet();
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

            if (isAdmin) {
                manageableProductIds = products.stream()
                        .map(product -> product.getId())
                        .collect(Collectors.toSet());
            } else {
                var currentUser = userRepository.findByEmail(authentication.getName());
                if (currentUser.isPresent()) {
                    manageableProductIds = productService.findBySeller(currentUser.get().getId()).stream()
                            .map(product -> product.getId())
                            .collect(Collectors.toSet());
                }
            }
        }

        model.addAttribute("manageableProductIds", manageableProductIds);
        return "products";
    }

    @GetMapping("/{id}")
    public String getProductById(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.findById(id));
        model.addAttribute("canManageProduct", productService.canCurrentUserManage(id));
        return "product-detail";
    }

    @GetMapping("/new")
    public String showCreateProductForm() {
        return "redirect:/products";
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getProductById(id));
        model.addAttribute("productId", id);
        return "product-form";
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping("")
    public String createProduct(
            @Valid @ModelAttribute("newProduct") ProductDTO dto,
            BindingResult bindingResult,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("productCreateError", "Please provide valid product details.");
            return "redirect:/products";
        }

        var user = userRepository.findByEmail(authentication.getName());
        if (user.isEmpty()) {
            redirectAttributes.addFlashAttribute("productCreateError", "Unable to resolve seller account for this session.");
            return "redirect:/products";
        }

        dto.setSellerId(user.get().getId());
        productService.create(dto);
        redirectAttributes.addFlashAttribute("productCreateSuccess", "Product created successfully.");
        return "redirect:/products";
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping("/edit/{id}")
    public String updateProduct(
            @PathVariable Long id,
            @Valid @ModelAttribute("product") ProductDTO dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("product", dto);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.product", bindingResult);
            redirectAttributes.addFlashAttribute("productId", id);
            return "redirect:/products/edit/" + id;
        }

        try {
            productService.updateProduct(id, dto);
            redirectAttributes.addFlashAttribute("productCreateSuccess", "Product updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("productCreateError", ex.getMessage());
            redirectAttributes.addFlashAttribute("product", dto);
            redirectAttributes.addFlashAttribute("productId", id);
            return "redirect:/products/edit/" + id;
        }

        return "redirect:/products";
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("productCreateSuccess", "Product deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("productCreateError", ex.getMessage());
        }
        return "redirect:/products";
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteProductPost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("productCreateSuccess", "Product deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("productCreateError", ex.getMessage());
        }
        return "redirect:/products";
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping("/{id}/edit")
    public String updateProductLegacy(
            @PathVariable Long id,
            @Valid @ModelAttribute("product") ProductDTO dto,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "product-form";
        }
        try {
            productService.updateProduct(id, dto);
            redirectAttributes.addFlashAttribute("productCreateSuccess", "Product updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("productCreateError", ex.getMessage());
        }
        return "redirect:/products";
    }
}