package com.wildbeyond.controller;

import com.wildbeyond.dto.ProductDTO;
import com.wildbeyond.repository.UserRepository;
import com.wildbeyond.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Product views (Thymeleaf MVC).
 * GET endpoints are public (browsing).
 * Create requires SELLER; update/delete require SELLER or ADMIN.
 * URL-level rules are in SecurityConfig; @PreAuthorize adds defense-in-depth.
 *
 * REST operations live in controller/rest/ProductRestController.
 */
@Controller
@RequestMapping({"/products", "/admin/products", "/seller/products", "/buyer/products"})
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;

    @GetMapping({"", "/"})
    public String getAllProducts(Model model,
                                 Authentication authentication,
                                 HttpServletRequest request) {
        String basePath = resolveProductsBasePath(request);
        if (!"/products".equals(basePath)) {
            assertRoleScopedAccess(basePath, authentication);
        }

        String canonicalBasePath = resolveCanonicalBasePath(authentication);
        if ("/products".equals(basePath) && isAuthenticated(authentication) && !"/products".equals(canonicalBasePath)) {
            return "redirect:" + canonicalBasePath;
        }

        var products = productService.findAll();
        model.addAttribute("products", products);
        ProductDTO newProduct = new ProductDTO();
        newProduct.setSellerId(0L);
        model.addAttribute("newProduct", newProduct);
        model.addAttribute("productsBasePath", basePath);
        model.addAttribute("cartBasePath", resolveCanonicalCartBasePath(authentication));

        Set<Long> manageableProductIds = Collections.emptySet();
        Set<Long> purchasableProductIds = Collections.emptySet();
        if (authentication != null && authentication.isAuthenticated()) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            boolean isBuyer = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_BUYER".equals(a.getAuthority()));
            boolean isSeller = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_SELLER".equals(a.getAuthority()));

            if (isAdmin) {
                manageableProductIds = products.stream()
                        .map(product -> product.getId())
                        .collect(Collectors.toSet());
            }

            if (isBuyer) {
                purchasableProductIds = products.stream().map(product -> product.getId()).collect(Collectors.toSet());
            } else {
                var currentUser = userRepository.findByEmail(authentication.getName());
                if (currentUser.isPresent()) {
                    Long currentUserId = currentUser.get().getId();
                    if (!isAdmin) {
                        manageableProductIds = productService.findBySeller(currentUserId).stream()
                                .map(product -> product.getId())
                                .collect(Collectors.toSet());
                    }

                    if (isSeller) {
                        Set<Long> myProductIds = productService.findBySeller(currentUserId).stream()
                                .map(product -> product.getId())
                                .collect(Collectors.toSet());
                        Set<Long> buyable = new HashSet<>(products.stream().map(product -> product.getId()).toList());
                        buyable.removeAll(myProductIds);
                        purchasableProductIds = buyable;
                    }
                }
            }
        }

        model.addAttribute("manageableProductIds", manageableProductIds);
        model.addAttribute("purchasableProductIds", purchasableProductIds);
        return "products";
    }

    @GetMapping("/{id}")
    public String getProductById(@PathVariable Long id,
                                 Model model,
                                 Authentication authentication,
                                 HttpServletRequest request) {
        String basePath = resolveProductsBasePath(request);
        if (!"/products".equals(basePath)) {
            assertRoleScopedAccess(basePath, authentication);
        }

        String canonicalBasePath = resolveCanonicalBasePath(authentication);
        if ("/products".equals(basePath) && isAuthenticated(authentication) && !"/products".equals(canonicalBasePath)) {
            return "redirect:" + canonicalBasePath + "/" + id;
        }

        model.addAttribute("product", productService.findById(id));
        model.addAttribute("canManageProduct", productService.canCurrentUserManage(id));
        model.addAttribute("canBuyProduct", productService.canCurrentUserBuy(id));
        model.addAttribute("productsBasePath", basePath);
        model.addAttribute("cartBasePath", resolveCanonicalCartBasePath(authentication));
        model.addAttribute("dashboardPath", resolveCanonicalDashboardPath(authentication));
        return "product-detail";
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getProductPhoto(@PathVariable Long id) {
        var product = productService.findById(id);
        if (product.getImageData() == null || product.getImageData().length == 0) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        String contentType = product.getImageContentType();
        if (contentType != null && !contentType.isBlank()) {
            mediaType = MediaType.parseMediaType(contentType);
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofHours(12)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .contentType(mediaType)
                .body(product.getImageData());
    }

    @GetMapping("/new")
    public String showCreateProductForm() {
        return "redirect:/products";
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable Long id,
                                  Model model,
                                  Authentication authentication,
                                  HttpServletRequest request) {
        String basePath = resolveProductsBasePath(request);
        if (!"/products".equals(basePath)) {
            assertRoleScopedAccess(basePath, authentication);
        }

        String canonicalBasePath = resolveCanonicalBasePath(authentication);
        if ("/products".equals(basePath) && isAuthenticated(authentication) && !"/products".equals(canonicalBasePath)) {
            return "redirect:" + canonicalBasePath + "/edit/" + id;
        }

        var product = productService.findById(id);
        model.addAttribute("product", productService.getProductById(id));
        model.addAttribute("productId", id);
        model.addAttribute("productImageAvailable", product.getImageData() != null && product.getImageData().length > 0);
        model.addAttribute("productsBasePath", basePath);
        return "product-form";
    }

    @PreAuthorize("hasRole('SELLER')")
    @PostMapping("")
    public String createProduct(
            @Valid @ModelAttribute("newProduct") ProductDTO dto,
            BindingResult bindingResult,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request) {
        String basePath = resolveProductsBasePath(request);
        if (!"/products".equals(basePath)) {
            assertRoleScopedAccess(basePath, authentication);
        }
        String redirectBasePath = "/products".equals(basePath) ? resolveCanonicalBasePath(authentication) : basePath;

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("productCreateError", "Please provide valid product details.");
            return "redirect:" + redirectBasePath;
        }

        var user = userRepository.findByEmail(authentication.getName());
        if (user.isEmpty()) {
            redirectAttributes.addFlashAttribute("productCreateError", "Unable to resolve seller account for this session.");
            return "redirect:" + redirectBasePath;
        }

        dto.setSellerId(user.get().getId());
        try {
            if (photo != null && !photo.isEmpty()) {
                productService.create(dto, photo);
            } else {
                productService.create(dto);
            }
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("productCreateError", ex.getMessage());
            return "redirect:" + redirectBasePath;
        }

        redirectAttributes.addFlashAttribute("productCreateSuccess", "Product created successfully.");
        return "redirect:" + redirectBasePath;
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping("/edit/{id}")
    public String updateProduct(
            @PathVariable Long id,
            @Valid @ModelAttribute("product") ProductDTO dto,
            BindingResult bindingResult,
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            RedirectAttributes redirectAttributes,
            Authentication authentication,
            HttpServletRequest request) {
        String basePath = resolveProductsBasePath(request);
        if (!"/products".equals(basePath)) {
            assertRoleScopedAccess(basePath, authentication);
        }
        String redirectBasePath = "/products".equals(basePath) ? resolveCanonicalBasePath(authentication) : basePath;

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("product", dto);
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.product", bindingResult);
            redirectAttributes.addFlashAttribute("productId", id);
            return "redirect:" + redirectBasePath + "/edit/" + id;
        }

        try {
            if (photo != null && !photo.isEmpty()) {
                productService.updateProduct(id, dto, photo);
            } else {
                productService.updateProduct(id, dto);
            }
            redirectAttributes.addFlashAttribute("productCreateSuccess", "Product updated successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("productCreateError", ex.getMessage());
            redirectAttributes.addFlashAttribute("product", dto);
            redirectAttributes.addFlashAttribute("productId", id);
            return "redirect:" + redirectBasePath + "/edit/" + id;
        }

        return "redirect:" + redirectBasePath;
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id,
                                RedirectAttributes redirectAttributes,
                                Authentication authentication,
                                HttpServletRequest request) {
        String basePath = resolveProductsBasePath(request);
        if (!"/products".equals(basePath)) {
            assertRoleScopedAccess(basePath, authentication);
        }
        String redirectBasePath = "/products".equals(basePath) ? resolveCanonicalBasePath(authentication) : basePath;

        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("productCreateSuccess", "Product deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("productCreateError", ex.getMessage());
        }
        return "redirect:" + redirectBasePath;
    }

    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @PostMapping("/{id}/delete")
    public String deleteProductPost(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes,
                                    Authentication authentication,
                                    HttpServletRequest request) {
        String basePath = resolveProductsBasePath(request);
        if (!"/products".equals(basePath)) {
            assertRoleScopedAccess(basePath, authentication);
        }
        String redirectBasePath = "/products".equals(basePath) ? resolveCanonicalBasePath(authentication) : basePath;

        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("productCreateSuccess", "Product deleted successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("productCreateError", ex.getMessage());
        }
        return "redirect:" + redirectBasePath;
    }

    private String resolveProductsBasePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return "/products";
        }

        if (uri.startsWith("/admin/products")) {
            return "/admin/products";
        }
        if (uri.startsWith("/seller/products")) {
            return "/seller/products";
        }
        if (uri.startsWith("/buyer/products")) {
            return "/buyer/products";
        }
        return "/products";
    }

    private String resolveCanonicalBasePath(Authentication authentication) {
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

    private String resolveCanonicalDashboardPath(Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            return "/dashboard";
        }

        if (hasRole(authentication, "ROLE_ADMIN")) {
            return "/admin";
        }
        if (hasRole(authentication, "ROLE_SELLER")) {
            return "/seller/dashboard";
        }
        if (hasRole(authentication, "ROLE_BUYER")) {
            return "/buyer/dashboard";
        }
        return "/dashboard";
    }

    private void assertRoleScopedAccess(String basePath, Authentication authentication) {
        if (!isAuthenticated(authentication)) {
            throw new ResponseStatusException(FORBIDDEN, "Authentication is required");
        }

        if ("/admin/products".equals(basePath) && !hasRole(authentication, "ROLE_ADMIN")) {
            throw new ResponseStatusException(FORBIDDEN, "Admin role required");
        }
        if ("/seller/products".equals(basePath) && !hasRole(authentication, "ROLE_SELLER")) {
            throw new ResponseStatusException(FORBIDDEN, "Seller role required");
        }
        if ("/buyer/products".equals(basePath) && !hasRole(authentication, "ROLE_BUYER")) {
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
