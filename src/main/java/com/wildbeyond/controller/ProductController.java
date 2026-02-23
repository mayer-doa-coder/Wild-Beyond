package com.wildbeyond.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    @GetMapping("")
    public void getAllProducts() {
        // ...method signature only
    }

    @GetMapping("/{id}")
    public void getProductById(@PathVariable Long id) {
        // ...method signature only
    }

    @PostMapping("")
    public void createProduct(/* @RequestBody ProductDTO productDTO */) {
        // ...method signature only
    }

    @PutMapping("/{id}")
    public void updateProduct(@PathVariable Long id/*, @RequestBody ProductDTO productDTO */) {
        // ...method signature only
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        // ...method signature only
    }
}