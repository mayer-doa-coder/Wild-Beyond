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
    public String getAllProducts(Model model) {
        model.addAttribute("products", productService.findAll());
        ProductDTO newProduct = new ProductDTO();
        newProduct.setSellerId(0L);
        model.addAttribute("newProduct", newProduct);
        return "products";
    }

    @GetMapping("/{id}")
    public String getProductById(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.findById(id));
        return "product-detail";
    }

    @GetMapping("/new")
    public String showCreateProductForm() {
        return "redirect:/products";
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
    @PostMapping("/{id}/edit")
    public String updateProduct(@PathVariable Long id) {
        return "redirect:/products";
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable Long id) {
        return "redirect:/products";
    }
}