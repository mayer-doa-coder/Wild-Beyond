package com.wildbeyond.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @GetMapping("")
    public void getAllOrders() {
        // ...method signature only
    }

    @GetMapping("/{id}")
    public void getOrderById(@PathVariable Long id) {
        // ...method signature only
    }

    @PostMapping("")
    public void createOrder(/* @RequestBody OrderDTO orderDTO */) {
        // ...method signature only
    }

    @PutMapping("/{id}")
    public void updateOrder(@PathVariable Long id/*, @RequestBody OrderDTO orderDTO */) {
        // ...method signature only
    }

    @DeleteMapping("/{id}")
    public void deleteOrder(@PathVariable Long id) {
        // ...method signature only
    }
}